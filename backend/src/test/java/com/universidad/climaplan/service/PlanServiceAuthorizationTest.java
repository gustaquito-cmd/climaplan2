package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanCreateRequest;
import com.universidad.climaplan.dto.PlanUpdateRequest;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @Test
    void crearSiempreGuardaElPlanComoPendiente() {
        Usuario propietario = usuario(7L);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(propietario));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PlanCreateRequest request = new PlanCreateRequest(
                "Caminata",
                "Tuxtla Gutiérrez",
                LocalDate.now().plusDays(3),
                "Senderismo",
                null
        );

        var response = planService.crear(request, "ana@example.com");

        assertEquals(EstadoPlan.PENDIENTE, response.estado());
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void actualizarPermiteCambiarElEstadoDelPlan() {
        Usuario propietario = usuario(7L);
        Plan plan = new Plan();
        plan.setUsuario(propietario);
        plan.setEstado(EstadoPlan.PENDIENTE);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(propietario));
        when(planRepository.findByIdAndUsuarioId(91L, 7L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PlanUpdateRequest request = new PlanUpdateRequest(
                "Caminata",
                "Tuxtla Gutiérrez",
                LocalDate.now().plusDays(3),
                "Senderismo",
                null,
                EstadoPlan.COMPLETADO
        );

        var response = planService.actualizar(91L, request, "ana@example.com", false);

        assertEquals(EstadoPlan.COMPLETADO, response.estado());
        verify(planRepository).save(plan);
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
