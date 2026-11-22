package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.DestinoFavoritoRequest;
import com.universidad.climaplan.dto.DestinoFavoritoResponse;
import com.universidad.climaplan.entity.DestinoFavorito;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.DestinoFavoritoRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DestinoFavoritoServiceTest {

    private final DestinoFavoritoRepository destinoFavoritoRepository = mock(DestinoFavoritoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final DestinoFavoritoService destinoFavoritoService =
            new DestinoFavoritoService(destinoFavoritoRepository, usuarioRepository);

    @Test
    void crearAsociaElDestinoAlUsuarioAutenticado() {
        Usuario usuario = usuario(7L);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(destinoFavoritoRepository.save(any(DestinoFavorito.class)))
                .thenAnswer(invocation -> {
                    DestinoFavorito destino = invocation.getArgument(0);
                    ReflectionTestUtils.setField(destino, "id", 12L);
                    return destino;
                });

        DestinoFavoritoResponse response = destinoFavoritoService.crear(request(
                "Tuxtla Gutiérrez", "México", 16.75, -93.11
        ), "ana@example.com");

        assertEquals(12L, response.id());
        assertEquals(7L, response.usuarioId());
        verify(destinoFavoritoRepository).save(any(DestinoFavorito.class));
    }

    @Test
    void listarDevuelveSoloLosDestinosDelUsuarioAutenticado() {
        Usuario usuario = usuario(7L);
        DestinoFavorito destino = destino(12L, usuario);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(destinoFavoritoRepository.findAllByUsuarioId(7L)).thenReturn(List.of(destino));

        List<DestinoFavoritoResponse> response =
                destinoFavoritoService.obtenerTodos("ana@example.com", false);

        assertEquals(List.of(12L), response.stream().map(DestinoFavoritoResponse::id).toList());
        verify(destinoFavoritoRepository).findAllByUsuarioId(7L);
        verify(destinoFavoritoRepository, never()).findAll();
    }

    @Test
    void obtenerConsultaElFavoritoPorIdYPropietario() {
        Usuario usuario = usuario(7L);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(destinoFavoritoRepository.findByIdAndUsuarioId(12L, 7L))
                .thenReturn(Optional.of(destino(12L, usuario)));

        DestinoFavoritoResponse response =
                destinoFavoritoService.obtenerPorId(12L, "ana@example.com", false);

        assertEquals(12L, response.id());
        assertEquals(7L, response.usuarioId());
        verify(destinoFavoritoRepository).findByIdAndUsuarioId(12L, 7L);
    }

    @Test
    void actualizarModificaUnicamenteLosCamposDelDestinoPropio() {
        Usuario usuario = usuario(7L);
        DestinoFavorito destino = destino(12L, usuario);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(destinoFavoritoRepository.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(destino));
        when(destinoFavoritoRepository.save(destino)).thenReturn(destino);

        DestinoFavoritoResponse response = destinoFavoritoService.actualizar(
                12L,
                request("Oaxaca", "México", 17.07, -96.72),
                "ana@example.com",
                false
        );

        assertEquals("Oaxaca", response.ciudad());
        assertEquals(7L, response.usuarioId());
        verify(destinoFavoritoRepository).save(destino);
    }

    @Test
    void eliminarBorraUnicamenteUnDestinoPropio() {
        Usuario usuario = usuario(7L);
        DestinoFavorito destino = destino(12L, usuario);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(destinoFavoritoRepository.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(destino));

        destinoFavoritoService.eliminar(12L, "ana@example.com", false);

        verify(destinoFavoritoRepository).delete(destino);
    }

    @Test
    void otroUsuarioNoPuedeConsultarUnDestinoAjeno() {
        Usuario usuario = usuario(8L);
        when(usuarioRepository.findByEmail("bea@example.com")).thenReturn(Optional.of(usuario));
        when(destinoFavoritoRepository.findByIdAndUsuarioId(12L, 8L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> destinoFavoritoService.obtenerPorId(12L, "bea@example.com", false)
        );

        verify(destinoFavoritoRepository).findByIdAndUsuarioId(12L, 8L);
        verify(destinoFavoritoRepository, never()).findById(12L);
    }

    @Test
    void administradorPuedeConsultarCualquierDestino() {
        Usuario propietario = usuario(7L);
        when(destinoFavoritoRepository.findById(12L))
                .thenReturn(Optional.of(destino(12L, propietario)));

        DestinoFavoritoResponse response =
                destinoFavoritoService.obtenerPorId(12L, "admin@example.com", true);

        assertEquals(7L, response.usuarioId());
        verify(usuarioRepository, never()).findByEmail("admin@example.com");
    }

    private static DestinoFavoritoRequest request(
            String ciudad,
            String pais,
            Double latitud,
            Double longitud
    ) {
        return new DestinoFavoritoRequest(ciudad, pais, latitud, longitud);
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private static DestinoFavorito destino(Long id, Usuario usuario) {
        DestinoFavorito destino = new DestinoFavorito();
        ReflectionTestUtils.setField(destino, "id", id);
        destino.setCiudad("Tuxtla Gutiérrez");
        destino.setPais("México");
        destino.setLatitud(16.75);
        destino.setLongitud(-93.11);
        destino.setUsuario(usuario);
        return destino;
    }
}
