package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.UsuarioResponse;
import com.universidad.climaplan.dto.auth.AuthResponse;
import com.universidad.climaplan.dto.auth.LoginRequest;
import com.universidad.climaplan.dto.auth.RegisterRequest;
import com.universidad.climaplan.entity.EstadoUsuario;
import com.universidad.climaplan.entity.Rol;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.EmailDuplicadoException;
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

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse registrar(RegisterRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailDuplicadoException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.ROLE_USER);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return crearAuthResponse(usuarioGuardado);
    }

    public AuthResponse iniciarSesion(LoginRequest request) {
        String email = normalizarEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
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
