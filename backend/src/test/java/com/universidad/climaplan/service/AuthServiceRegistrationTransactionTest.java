package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.auth.RegisterRequest;
import com.universidad.climaplan.dto.auth.TwoFactorRequiredResponse;
import com.universidad.climaplan.dto.auth.VerifyEmailRequest;
import com.universidad.climaplan.dto.auth.VerifyTwoFactorRequest;
import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import com.universidad.climaplan.exception.EmailVerificationFailedException;
import com.universidad.climaplan.exception.TwoFactorVerificationFailedException;
import com.universidad.climaplan.repository.EmailVerificationCodeRepository;
import com.universidad.climaplan.repository.TwoFactorChallengeRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.security.JwtService;
import com.universidad.climaplan.security.OtpCodeService;
import com.universidad.climaplan.entity.EstadoUsuario;
import com.universidad.climaplan.entity.Rol;
import com.universidad.climaplan.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({AuthService.class, EmailVerificationService.class, TwoFactorService.class, OtpCodeService.class})
@TestPropertySource(properties = "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=")
class AuthServiceRegistrationTransactionTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailVerificationCodeRepository codeRepository;

    @Autowired
    private TwoFactorChallengeRepository challengeRepository;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmailService emailService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void smtpFailureRollsBackNewUserAndVerificationCode() {
        String email = "smtp-failure@example.com";
        when(passwordEncoder.encode("password123")).thenReturn("password-hash");
        when(emailService.isConfigured()).thenReturn(true);
        doThrow(new EmailDeliveryUnavailableException())
                .when(emailService).sendVerificationCode(eq(email), anyString());

        assertThrows(EmailDeliveryUnavailableException.class, () ->
                authService.registrar(new RegisterRequest("Test User", email, "password123")));

        assertFalse(usuarioRepository.existsByEmail(email));
        assertEquals(0, codeRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoFactorSmtpFailureDoesNotLeaveChallengePersisted() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Verified User");
        usuario.setEmail("two-factor-failure@example.com");
        usuario.setPasswordHash("password-hash");
        usuario.setRol(Rol.ROLE_USER);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setEmailVerified(true);
        Usuario savedUser = usuarioRepository.save(usuario);

        when(emailService.isConfigured()).thenReturn(true);
        doThrow(new EmailDeliveryUnavailableException())
                .when(emailService).sendTwoFactorCode(eq(savedUser.getEmail()), anyString());

        assertThrows(EmailDeliveryUnavailableException.class, () ->
                twoFactorService.createChallenge(savedUser));

        assertEquals(0, challengeRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedTwoFactorAttemptsPersistThroughAuthServiceAndExhaustChallenge() {
        Usuario user = createUser("two-factor-attempts@example.com", true);
        when(emailService.isConfigured()).thenReturn(true);
        TwoFactorRequiredResponse response = twoFactorService.createChallenge(user);
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendTwoFactorCode(eq(user.getEmail()), otp.capture());
        String wrongCode = otp.getValue().equals("000000") ? "000001" : "000000";
        UUID challengeId = UUID.fromString(response.challengeId());

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThrows(TwoFactorVerificationFailedException.class, () ->
                    authService.verificarDosFactores(new VerifyTwoFactorRequest(challengeId, wrongCode)));
            assertEquals(attempt, challengeRepository.findById(challengeId).orElseThrow().getAttempts());
        }

        assertTrue(challengeRepository.findById(challengeId).orElseThrow().isUsed());
        assertThrows(TwoFactorVerificationFailedException.class, () ->
                authService.verificarDosFactores(new VerifyTwoFactorRequest(challengeId, otp.getValue())));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedEmailVerificationAttemptsPersistThroughAuthService() {
        Usuario user = createUser("email-attempts@example.com", false);
        when(emailService.isConfigured()).thenReturn(true);
        emailVerificationService.sendVerificationCode(user);
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(eq(user.getEmail()), otp.capture());
        String wrongCode = otp.getValue().equals("000000") ? "000001" : "000000";

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThrows(EmailVerificationFailedException.class, () ->
                    authService.verificarCorreo(new VerifyEmailRequest(user.getEmail(), wrongCode)));
            assertEquals(attempt, codeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(user.getId())
                    .orElseThrow().getAttempts());
        }

        assertTrue(codeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow().isUsed());
        assertThrows(EmailVerificationFailedException.class, () ->
                authService.verificarCorreo(new VerifyEmailRequest(user.getEmail(), otp.getValue())));
    }

    @AfterEach
    void cleanUpAttemptTests() {
        usuarioRepository.findByEmail("two-factor-attempts@example.com").ifPresent(user -> {
            challengeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(user.getId())
                    .ifPresent(challengeRepository::delete);
            usuarioRepository.delete(user);
        });
        usuarioRepository.findByEmail("email-attempts@example.com").ifPresent(user -> {
            codeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(user.getId())
                    .ifPresent(codeRepository::delete);
            usuarioRepository.delete(user);
        });
    }

    private Usuario createUser(String email, boolean verified) {
        Usuario user = new Usuario();
        user.setNombre("Test User");
        user.setEmail(email);
        user.setPasswordHash("password-hash");
        user.setRol(Rol.ROLE_USER);
        user.setEstado(EstadoUsuario.ACTIVO);
        user.setEmailVerified(verified);
        return usuarioRepository.save(user);
    }
}
