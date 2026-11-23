package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.DestinoFavoritoRequest;
import com.universidad.climaplan.dto.DestinoFavoritoResponse;
import com.universidad.climaplan.entity.DestinoFavorito;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.DestinoFavoritoRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DestinoFavoritoService {

    private final DestinoFavoritoRepository destinoFavoritoRepository;
    private final UsuarioRepository usuarioRepository;

    public DestinoFavoritoService(
            DestinoFavoritoRepository destinoFavoritoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.destinoFavoritoRepository = destinoFavoritoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<DestinoFavoritoResponse> obtenerTodos(String email, boolean administrador) {
        List<DestinoFavorito> destinos = administrador
                ? destinoFavoritoRepository.findAll()
                : destinoFavoritoRepository.findAllByUsuarioId(buscarUsuario(email).getId());
        return destinos.stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DestinoFavoritoResponse obtenerPorId(Long id, String email, boolean administrador) {
        return convertirAResponse(buscarDestino(id, email, administrador));
    }

    public DestinoFavoritoResponse crear(DestinoFavoritoRequest request, String email) {
        DestinoFavorito destino = new DestinoFavorito();
        aplicarRequest(destino, request);
        destino.setUsuario(buscarUsuario(email));
        return convertirAResponse(destinoFavoritoRepository.save(destino));
    }

    public DestinoFavoritoResponse actualizar(
            Long id,
            DestinoFavoritoRequest request,
            String email,
            boolean administrador
    ) {
        DestinoFavorito destino = buscarDestino(id, email, administrador);
        aplicarRequest(destino, request);
        return convertirAResponse(destinoFavoritoRepository.save(destino));
    }

    public void eliminar(Long id, String email, boolean administrador) {
        destinoFavoritoRepository.delete(buscarDestino(id, email, administrador));
    }

    private DestinoFavorito buscarDestino(Long id, String email, boolean administrador) {
        return (administrador
                ? destinoFavoritoRepository.findById(id)
                : destinoFavoritoRepository.findByIdAndUsuarioId(id, buscarUsuario(email).getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el destino favorito con ID " + id
                ));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario autenticado"
                ));
    }

    private void aplicarRequest(DestinoFavorito destino, DestinoFavoritoRequest request) {
        destino.setCiudad(request.ciudad());
        destino.setPais(request.pais());
        destino.setLatitud(request.latitud());
        destino.setLongitud(request.longitud());
    }

    private DestinoFavoritoResponse convertirAResponse(DestinoFavorito destino) {
        return new DestinoFavoritoResponse(
                destino.getId(),
                destino.getCiudad(),
                destino.getPais(),
                destino.getLatitud(),
                destino.getLongitud(),
                destino.getUsuario().getId()
        );
    }
}
