package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.UsuarioRequest;
import com.universidad.climaplan.dto.UsuarioResponse;
import com.universidad.climaplan.dto.UsuarioUpdateRequest;
import com.universidad.climaplan.entity.EstadoUsuario;
import com.universidad.climaplan.entity.Rol;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.EmailDuplicadoException;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return convertirAResponse(buscarUsuario(id));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(this::convertirAResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario autenticado"
                ));
    }

    public UsuarioResponse crear(UsuarioRequest request) {
        String email = normalizarEmail(request.email());
        validarEmailDisponible(email);

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.ROLE_USER);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        return convertirAResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarUsuario(id);
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailDuplicadoException("El email ya está registrado");
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        return convertirAResponse(usuarioRepository.save(usuario));
    }

    public void desactivar(Long id) {
        Usuario usuario = buscarUsuario(id);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario con ID " + id
                ));
    }

    private void validarEmailDisponible(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailDuplicadoException("El email ya está registrado");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UsuarioResponse convertirAResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getEstado()
        );
    }
}
