package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.auth.AuthOperationResponse;
import com.universidad.climaplan.dto.auth.AuthLoginResponse;
import com.universidad.climaplan.dto.auth.AuthResponse;
import com.universidad.climaplan.dto.auth.LoginRequest;
import com.universidad.climaplan.dto.auth.RegisterRequest;
import com.universidad.climaplan.dto.auth.TwoFactorRequiredResponse;
import com.universidad.climaplan.dto.auth.VerifyTwoFactorRequest;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.EmailNotVerifiedException;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private TwoFactorService twoFactorService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                emailVerificationService,
                twoFactorService,
                false
        );
    }

    @Test
    void registrationCreatesUnverifiedAccountAndSendsCode() {
        when(passwordEncoder.encode("password123")).thenReturn("password-hash");
        when(usuarioRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        AuthOperationResponse response = authService.registrar(
                new RegisterRequest("Test User", "User@example.com", "password123")
        );

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(emailVerificationService).sendVerificationCode(usuarioCaptor.getValue());
        assertFalse(usuarioCaptor.getValue().isEmailVerified());
        assertEquals("user@example.com", usuarioCaptor.getValue().getEmail());
        assertTrue(response.message().contains("verificación"));
    }

    @Test
    void duplicateRegistrationReturnsSameGenericResponseAndMayResendOtp() {
        when(usuarioRepository.existsByEmail("user@example.com")).thenReturn(true);

        AuthOperationResponse response = authService.registrar(
                new RegisterRequest("Test User", "user@example.com", "password123")
        );

        verify(emailVerificationService).resend("user@example.com");
        verify(usuarioRepository, org.mockito.Mockito.never()).save(any(Usuario.class));
        assertEquals(
                "Si el correo puede registrarse, recibirás un código de verificación.",
                response.message()
        );
    }

    @Test
    void loginRejectsCorrectCredentialsUntilEmailIsVerified() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@example.com");
        usuario.setEmailVerified(false);
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(EmailNotVerifiedException.class, () ->
                authService.iniciarSesion(new LoginRequest("user@example.com", "password123")));
    }

    @Test
    void unverifiedEmailIsRejectedBeforeEnabledTwoFactor() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@example.com");
        usuario.setEmailVerified(false);
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));

        AuthService twoFactorAuthService = createService(true);
        assertThrows(EmailNotVerifiedException.class, () ->
                twoFactorAuthService.iniciarSesion(new LoginRequest("user@example.com", "password123")));
        verify(twoFactorService, never()).createChallenge(any());
    }

    @Test
    void loginAllowsLegacyUserWithNoVerificationValue() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@example.com");
        usuario.setEmailVerified(null);
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarAccessToken(usuario)).thenReturn("legacy-jwt");
        when(jwtService.obtenerExpiracionMs()).thenReturn(3_600_000L);

        AuthResponse response = assertInstanceOf(AuthResponse.class, authService.iniciarSesion(
                new LoginRequest("user@example.com", "password123")
        ));

        assertEquals("legacy-jwt", response.accessToken());
    }

    @Test
    void loginReturnsJwtAfterEmailIsVerified() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Test User");
        usuario.setEmail("user@example.com");
        usuario.setEmailVerified(true);
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarAccessToken(usuario)).thenReturn("jwt-token");
        when(jwtService.obtenerExpiracionMs()).thenReturn(3_600_000L);

        AuthResponse response = assertInstanceOf(AuthResponse.class, authService.iniciarSesion(
                new LoginRequest("user@example.com", "password123")
        ));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("user@example.com", response.usuario().email());
        verify(authenticationManager).authenticate(any());
        verify(twoFactorService, never()).createChallenge(usuario);
    }

    @Test
    void enabledTwoFactorLoginReturnsChallengeWithoutGeneratingJwt() {
        Usuario usuario = verifiedUser();
        TwoFactorRequiredResponse challenge =
                new TwoFactorRequiredResponse(true, UUID.randomUUID().toString(), "Código enviado");
        when(usuarioRepository.findByEmail("user@example.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.createChallenge(usuario)).thenReturn(challenge);

        AuthService twoFactorAuthService = createService(true);
        AuthLoginResponse response = twoFactorAuthService.iniciarSesion(
                new LoginRequest("user@example.com", "password123")
        );

        assertEquals(challenge, response);
        verify(jwtService, never()).generarAccessToken(any());
        verify(twoFactorService).createChallenge(usuario);
    }

    @Test
    void successfulTwoFactorVerificationReturnsNormalJwtResponse() {
        Usuario usuario = verifiedUser();
        UUID challengeId = UUID.randomUUID();
        when(twoFactorService.verify(challengeId, "123456")).thenReturn(usuario);
        when(jwtService.generarAccessToken(usuario)).thenReturn("jwt-after-2fa");
        when(jwtService.obtenerExpiracionMs()).thenReturn(3_600_000L);

        AuthResponse response = authService.verificarDosFactores(
                new VerifyTwoFactorRequest(challengeId, "123456")
        );

        assertEquals("jwt-after-2fa", response.accessToken());
        verify(jwtService).generarAccessToken(usuario);
    }

    private Usuario verifiedUser() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Test User");
        usuario.setEmail("user@example.com");
        usuario.setEmailVerified(true);
        return usuario;
    }

    private AuthService createService(boolean enabled) {
        return new AuthService(
                usuarioRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                emailVerificationService,
                twoFactorService,
                enabled
        );
    }
}
