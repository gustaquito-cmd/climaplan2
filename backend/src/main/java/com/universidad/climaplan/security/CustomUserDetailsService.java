package com.universidad.climaplan.security;

import com.universidad.climaplan.entity.EstadoUsuario;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }

        return User.withUsername(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(usuario.getRol().name())
                .build();
    }
}
