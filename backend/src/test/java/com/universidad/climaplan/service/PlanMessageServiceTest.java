package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanMessageRequest;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanMessage;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanMessageRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanMessageServiceTest {

    @Mock
    private PlanMessageRepository planMessageRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanMessageService planMessageService;

    private Usuario sender;
    private Plan plan;

    @BeforeEach
    void setUp() {
        sender = usuario(9L, "Carlos", "carlos@example.com");
        Usuario owner = usuario(7L, "Gustavo", "gustavo@example.com");
        plan = new Plan();
        ReflectionTestUtils.setField(plan, "id", 91L);
        plan.setTitulo("Viaje a San Cristóbal");
        plan.setCiudad("San Cristóbal");
        plan.setFecha(LocalDate.now().plusDays(3));
        plan.setTipoActividad("Viaje");
        plan.setEstado(EstadoPlan.PENDIENTE);
        plan.setUsuario(owner);
    }

    @Test
    void acceptedMemberEnvíaYPersisteMensajeConIdentidadYHoraDelServidor() {
        when(planService.obtenerPlanLegible(91L, sender.getEmail())).thenReturn(plan);
        when(usuarioRepository.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
        when(planMessageRepository.save(any(PlanMessage.class))).thenAnswer(invocation -> {
            PlanMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 501L);
            return message;
        });
        Instant before = Instant.now();

        var response = planMessageService.enviar(
                91L,
                new PlanMessageRequest("  Salimos a las siete?  "),
                sender.getEmail()
        );

        Instant after = Instant.now();
        assertEquals(501L, response.id());
        assertEquals(91L, response.planId());
        assertEquals(9L, response.senderId());
        assertEquals("Carlos", response.senderName());
        assertEquals("Salimos a las siete?", response.content());
        assertNotNull(response.sentAt());
        assertTrue(!response.sentAt().isBefore(before) && !response.sentAt().isAfter(after));
        verify(planMessageRepository).save(any(PlanMessage.class));
    }

    @Test
    void historialAutorizaPlanYDevuelveLosMensajesRecientesEnOrdenCronologico() {
        PlanMessage recent = message(502L, "Ana", "ana@example.com", "A las siete", Instant.parse("2026-11-24T18:02:00Z"));
        PlanMessage earlier = message(501L, "Carlos", sender.getEmail(), "¿A qué hora?", Instant.parse("2026-11-24T18:01:00Z"));
        when(planService.obtenerPlanLegible(91L, sender.getEmail())).thenReturn(plan);
        when(planMessageRepository.findAllByPlanIdOrderBySentAtDescIdDesc(
                anyLong(),
                any()
        )).thenReturn(new PageImpl<>(List.of(recent, earlier), PageRequest.of(0, 50), 2));

        var history = planMessageService.historial(91L, sender.getEmail(), 0, 50);

        assertEquals(2, history.getContent().size());
        assertEquals(501L, history.getContent().get(0).id());
        assertEquals(502L, history.getContent().get(1).id());
        verify(planService).obtenerPlanLegible(91L, sender.getEmail());
    }

    @Test
    void noPersisteMensajesCuandoElUsuarioNoTieneAccesoAlPlan() {
        when(planService.obtenerPlanLegible(91L, sender.getEmail()))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el plan"));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planMessageService.enviar(
                        91L,
                        new PlanMessageRequest("Mensaje privado"),
                        sender.getEmail()
                )
        );

        verify(usuarioRepository, never()).findByEmail(anyString());
        verify(planMessageRepository, never()).save(any(PlanMessage.class));
    }

    @Test
    void rechazaContenidoVacioYSuperiorAlLimite() {
        when(planService.obtenerPlanLegible(91L, sender.getEmail())).thenReturn(plan);

        assertThrows(
                IllegalArgumentException.class,
                () -> planMessageService.enviar(91L, new PlanMessageRequest("   "), sender.getEmail())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> planMessageService.enviar(
                        91L,
                        new PlanMessageRequest("x".repeat(1001)),
                        sender.getEmail()
                )
        );

        verify(planMessageRepository, never()).save(any(PlanMessage.class));
    }

    private PlanMessage message(Long id, String nombre, String email, String content, Instant sentAt) {
        Usuario usuario = usuario(id, nombre, email);
        PlanMessage message = new PlanMessage();
        ReflectionTestUtils.setField(message, "id", id);
        message.setPlan(plan);
        message.setUsuario(usuario);
        message.setContent(content);
        message.setSentAt(sentAt);
        return message;
    }

    private static Usuario usuario(Long id, String nombre, String email) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        return usuario;
    }
}
