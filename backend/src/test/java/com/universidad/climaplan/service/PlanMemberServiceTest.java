package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanMemberInviteRequest;
import com.universidad.climaplan.dto.PlanOwnership;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanMember;
import com.universidad.climaplan.entity.PlanMemberStatus;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.ConflictoRecursoException;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanMemberRepository;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanMemberServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanMemberRepository planMemberRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanMemberService planMemberService;

    private Usuario owner;
    private Usuario invited;
    private Plan plan;

    @BeforeEach
    void setUp() {
        owner = usuario(7L, "Gustavo", "gustavo@example.com");
        invited = usuario(9L, "Carlos", "carlos@example.com");
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
    void propietarioInvitaUsuarioRegistrado() {
        when(usuarioRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(planRepository.findByIdAndUsuarioId(91L, 7L)).thenReturn(Optional.of(plan));
        when(usuarioRepository.findByEmail(invited.getEmail())).thenReturn(Optional.of(invited));
        when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 9L)).thenReturn(Optional.empty());
        when(planMemberRepository.save(any(PlanMember.class)))
                .thenAnswer(invocation -> {
                    PlanMember member = invocation.getArgument(0);
                    ReflectionTestUtils.setField(member, "id", 27L);
                    return member;
                });

        var response = planMemberService.invitar(
                91L,
                new PlanMemberInviteRequest(invited.getEmail()),
                owner.getEmail()
        );

        assertEquals(9L, response.id());
        assertEquals("Carlos", response.nombre());
        assertEquals(invited.getEmail(), response.email());
        assertEquals(PlanMemberStatus.PENDING, response.estado());
        assertEquals(PlanOwnership.MEMBER, response.rol());
    }

    @Test
    void noPermiteAutoInvitacionNiInvitacionDuplicada() {
        when(usuarioRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(planRepository.findByIdAndUsuarioId(91L, 7L)).thenReturn(Optional.of(plan));
        assertThrows(
                ConflictoRecursoException.class,
                () -> planMemberService.invitar(
                        91L,
                        new PlanMemberInviteRequest(owner.getEmail()),
                        owner.getEmail()
                )
        );

        PlanMember pending = miembro(invited, PlanMemberStatus.PENDING);
        when(usuarioRepository.findByEmail(invited.getEmail())).thenReturn(Optional.of(invited));
        when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 9L)).thenReturn(Optional.of(pending));
        assertThrows(
                ConflictoRecursoException.class,
                () -> planMemberService.invitar(
                        91L,
                        new PlanMemberInviteRequest(invited.getEmail()),
                        owner.getEmail()
                )
        );
        verify(planMemberRepository, never()).save(any(PlanMember.class));
    }

    @Test
    void noDuplicaUnParticipanteQueYaAcepto() {
        when(usuarioRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(planRepository.findByIdAndUsuarioId(91L, 7L)).thenReturn(Optional.of(plan));
        when(usuarioRepository.findByEmail(invited.getEmail())).thenReturn(Optional.of(invited));
        when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 9L))
                .thenReturn(Optional.of(miembro(invited, PlanMemberStatus.ACCEPTED)));

        assertThrows(
                ConflictoRecursoException.class,
                () -> planMemberService.invitar(
                        91L,
                        new PlanMemberInviteRequest(invited.getEmail()),
                        owner.getEmail()
                )
        );

        verify(planMemberRepository, never()).save(any(PlanMember.class));
    }

    @Test
    void usuarioDistintoAlPropietarioNoPuedeInvitar() {
        Usuario stranger = usuario(12L, "Ana", "ana@example.com");
        when(usuarioRepository.findByEmail(stranger.getEmail())).thenReturn(Optional.of(stranger));
        when(planRepository.findByIdAndUsuarioId(91L, 12L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planMemberService.invitar(
                        91L,
                        new PlanMemberInviteRequest(invited.getEmail()),
                        stranger.getEmail()
                )
        );

        verify(usuarioRepository, never()).findByEmail(invited.getEmail());
        verify(planMemberRepository, never()).save(any(PlanMember.class));
    }

    @Test
    void invitacionPendienteSoloSeDevuelveAlUsuarioInvitado() {
        PlanMember pending = miembro(invited, PlanMemberStatus.PENDING);
        pending.setInvitedAt(LocalDateTime.now());
        when(usuarioRepository.findByEmail(invited.getEmail())).thenReturn(Optional.of(invited));
        when(planMemberRepository.findAllByUsuarioIdAndStatusOrderByInvitedAtDesc(
                9L,
                PlanMemberStatus.PENDING
        )).thenReturn(List.of(pending));

        var invitations = planMemberService.obtenerInvitacionesPendientes(invited.getEmail());

        assertEquals(1, invitations.size());
        assertEquals(27L, invitations.getFirst().id());
        assertEquals(91L, invitations.getFirst().planId());
        assertEquals("Viaje a San Cristóbal", invitations.getFirst().tituloPlan());
        assertEquals("Gustavo", invitations.getFirst().propietarioNombre());
        assertNotNull(invitations.getFirst().invitedAt());
    }

    @Test
    void invitadoAceptaYRechazaYNoPuedeResolverDosVeces() {
        when(usuarioRepository.findByEmail(invited.getEmail())).thenReturn(Optional.of(invited));
        PlanMember accepted = miembro(invited, PlanMemberStatus.PENDING);
        when(planMemberRepository.findByIdAndUsuarioId(27L, 9L))
                .thenReturn(Optional.of(accepted));
        when(planMemberRepository.save(any(PlanMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        planMemberService.aceptarInvitacion(27L, invited.getEmail());

        assertEquals(PlanMemberStatus.ACCEPTED, accepted.getStatus());
        assertNotNull(accepted.getJoinedAt());
        assertThrows(
                ConflictoRecursoException.class,
                () -> planMemberService.aceptarInvitacion(27L, invited.getEmail())
        );

        PlanMember rejected = miembro(invited, PlanMemberStatus.PENDING);
        when(planMemberRepository.findByIdAndUsuarioId(28L, 9L))
                .thenReturn(Optional.of(rejected));
        planMemberService.rechazarInvitacion(28L, invited.getEmail());

        assertEquals(PlanMemberStatus.REJECTED, rejected.getStatus());
        assertNull(rejected.getJoinedAt());
        assertThrows(
                ConflictoRecursoException.class,
                () -> planMemberService.rechazarInvitacion(28L, invited.getEmail())
        );
    }

    @Test
    void otraCuentaNoPuedeAceptarInvitacionAjena() {
        Usuario stranger = usuario(12L, "Ana", "ana@example.com");
        when(usuarioRepository.findByEmail(stranger.getEmail())).thenReturn(Optional.of(stranger));
        when(planMemberRepository.findByIdAndUsuarioId(27L, 12L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planMemberService.aceptarInvitacion(27L, stranger.getEmail())
        );
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planMemberService.rechazarInvitacion(27L, stranger.getEmail())
        );
    }

    @Test
    void participanteAceptadoPuedeListarParticipantesPeroPendingNo() {
        PlanMember accepted = miembro(invited, PlanMemberStatus.ACCEPTED);
        when(planService.obtenerPlanLegible(91L, invited.getEmail()))
                .thenReturn(plan)
                .thenThrow(new RecursoNoEncontradoException("No se encontró el plan"));
        when(planMemberRepository.findAllByPlanIdOrderByInvitedAtAsc(91L)).thenReturn(List.of(accepted));

        var members = planMemberService.listarParticipantes(91L, invited.getEmail());

        assertEquals(2, members.size());
        assertEquals(PlanOwnership.OWNER, members.getFirst().rol());
        assertEquals(PlanOwnership.MEMBER, members.getLast().rol());
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planMemberService.listarParticipantes(91L, invited.getEmail())
        );
    }

    @Test
    void propietarioPuedeEliminarParticipantePeroNoAElMismo() {
        when(usuarioRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(planRepository.findByIdAndUsuarioId(91L, 7L)).thenReturn(Optional.of(plan));
        PlanMember accepted = miembro(invited, PlanMemberStatus.ACCEPTED);
        when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 9L)).thenReturn(Optional.of(accepted));

        planMemberService.eliminarParticipante(91L, 9L, owner.getEmail());

        verify(planMemberRepository).delete(accepted);
        assertThrows(
                ConflictoRecursoException.class,
                () -> planMemberService.eliminarParticipante(91L, 7L, owner.getEmail())
        );
    }

    @Test
    void usuarioAjenoNoPuedeEliminarParticipante() {
        Usuario stranger = usuario(12L, "Ana", "ana@example.com");
        when(usuarioRepository.findByEmail(stranger.getEmail())).thenReturn(Optional.of(stranger));
        when(planRepository.findByIdAndUsuarioId(91L, 12L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planMemberService.eliminarParticipante(91L, 9L, stranger.getEmail())
        );

        verify(planMemberRepository, never()).delete(any(PlanMember.class));
    }

    private PlanMember miembro(Usuario usuario, PlanMemberStatus status) {
        PlanMember member = new PlanMember();
        ReflectionTestUtils.setField(member, "id", 27L);
        member.setPlan(plan);
        member.setUsuario(usuario);
        member.setStatus(status);
        member.setInvitedAt(LocalDateTime.now());
        return member;
    }

    private static Usuario usuario(Long id, String nombre, String email) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        return usuario;
    }
}
