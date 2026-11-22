package com.universidad.climaplan.service;

import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class PlanServiceAuthorizationTest {

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PlanService planService = new PlanService(planRepository, usuarioRepository);

    @Test
    void usuarioNoPuedeConsultarPlanDeOtroUsuario() {
        Usuario usuarioAutenticado = new Usuario();
        ReflectionTestUtils.setField(usuarioAutenticado, "id", 7L);
        when(usuarioRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(usuarioAutenticado));
        when(planRepository.findByIdAndUsuarioId(91L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planService.obtenerPorId(91L, "ana@example.com", false)
        );

        verify(planRepository).findByIdAndUsuarioId(91L, 7L);
        verify(planRepository, never()).findById(91L);
    }
}
