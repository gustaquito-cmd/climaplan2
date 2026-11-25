package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.UsuarioResponse;
import com.universidad.climaplan.dto.auth.AuthOperationResponse;
import com.universidad.climaplan.dto.auth.AuthLoginResponse;
import com.universidad.climaplan.dto.auth.AuthResponse;
import com.universidad.climaplan.dto.auth.LoginRequest;
import com.universidad.climaplan.dto.auth.RegisterRequest;
import com.universidad.climaplan.dto.auth.ResendVerificationRequest;
import com.universidad.climaplan.dto.auth.VerifyEmailRequest;
import com.universidad.climaplan.dto.auth.VerifyTwoFactorRequest;
import com.universidad.climaplan.entity.EstadoUsuario;
import com.universidad.climaplan.entity.Rol;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.EmailNotVerifiedException;
import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import com.universidad.climaplan.exception.EmailVerificationFailedException;
import com.universidad.climaplan.exception.TwoFactorVerificationFailedException;
import org.springframework.beans.factory.annotation.Value;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final TwoFactorService twoFactorService;
    private final boolean twoFactorEnabled;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmailVerificationService emailVerificationService,
            TwoFactorService twoFactorService,
            @Value("${app.auth.two-factor-enabled:false}") boolean twoFactorEnabled
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
        this.twoFactorService = twoFactorService;
        this.twoFactorEnabled = twoFactorEnabled;
    }

    @Transactional(rollbackFor = EmailDeliveryUnavailableException.class)
    public AuthOperationResponse registrar(RegisterRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            emailVerificationService.resend(email);
            return new AuthOperationResponse(
                    "Si el correo puede registrarse, recibirás un código de verificación."
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.ROLE_USER);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setEmailVerified(false);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        emailVerificationService.sendVerificationCode(usuarioGuardado);
        return new AuthOperationResponse(
                "Si el correo puede registrarse, recibirás un código de verificación."
        );
    }

    public AuthLoginResponse iniciarSesion(LoginRequest request) {
        String email = normalizarEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!usuario.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        if (twoFactorEnabled) {
            return twoFactorService.createChallenge(usuario);
        }
        return crearAuthResponse(usuario);
    }

    @Transactional(noRollbackFor = EmailVerificationFailedException.class)
    public AuthOperationResponse verificarCorreo(VerifyEmailRequest request) {
        emailVerificationService.verify(request.email(), request.code());
        return new AuthOperationResponse("Correo verificado. Ya puedes iniciar sesión.");
    }

    public AuthOperationResponse reenviarVerificacion(ResendVerificationRequest request) {
        emailVerificationService.resend(request.email());
        return new AuthOperationResponse(
                "Si la cuenta necesita verificación, recibirás un código en tu correo."
        );
    }

    @Transactional(noRollbackFor = TwoFactorVerificationFailedException.class)
    public AuthResponse verificarDosFactores(VerifyTwoFactorRequest request) {
        Usuario usuario = twoFactorService.verify(request.challengeId(), request.code());
        return crearAuthResponse(usuario);
    }

    private AuthResponse crearAuthResponse(Usuario usuario) {
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getEstado()
        );
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                "Bearer",
                jwtService.obtenerExpiracionMs(),
                usuarioResponse
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
