package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanInvitationResponse;
import com.universidad.climaplan.dto.PlanMemberInviteRequest;
import com.universidad.climaplan.dto.PlanMemberResponse;
import com.universidad.climaplan.dto.PlanOwnership;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanMember;
import com.universidad.climaplan.entity.PlanMemberStatus;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.ConflictoRecursoException;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanMemberRepository;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PlanMemberService {

    private final PlanRepository planRepository;
    private final PlanMemberRepository planMemberRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanService planService;

    public PlanMemberService(
            PlanRepository planRepository,
            PlanMemberRepository planMemberRepository,
            UsuarioRepository usuarioRepository,
            PlanService planService
    ) {
        this.planRepository = planRepository;
        this.planMemberRepository = planMemberRepository;
        this.usuarioRepository = usuarioRepository;
        this.planService = planService;
    }

    public PlanMemberResponse invitar(
            Long planId,
            PlanMemberInviteRequest request,
            String emailPropietario
    ) {
        Plan plan = buscarPlanPropietario(planId, emailPropietario);
        Usuario invitado = usuarioRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró un usuario registrado con ese email"
                ));
        if (invitado.getId().equals(plan.getUsuario().getId())) {
            throw new ConflictoRecursoException("El propietario no puede invitarse a sí mismo");
        }

        PlanMember member = planMemberRepository.findByPlanIdAndUsuarioId(planId, invitado.getId())
                .orElseGet(PlanMember::new);
        if (member.getId() != null && member.getStatus() != PlanMemberStatus.REJECTED) {
            throw new ConflictoRecursoException("El usuario ya participa o tiene una invitación pendiente");
        }

        member.setPlan(plan);
        member.setUsuario(invitado);
        member.setStatus(PlanMemberStatus.PENDING);
        member.setInvitedAt(LocalDateTime.now());
        member.setJoinedAt(null);
        return convertirAResponse(planMemberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<PlanMemberResponse> listarParticipantes(Long planId, String email) {
        Plan plan = planService.obtenerPlanLegible(planId, email);
        List<PlanMemberResponse> participantes = new ArrayList<>();
        Usuario propietario = plan.getUsuario();
        if (propietario != null) {
            participantes.add(new PlanMemberResponse(
                    propietario.getId(),
                    propietario.getNombre(),
                    propietario.getEmail(),
                    PlanMemberStatus.ACCEPTED,
                    PlanOwnership.OWNER
            ));
        }
        planMemberRepository.findAllByPlanIdOrderByInvitedAtAsc(planId).stream()
                .map(this::convertirAResponse)
                .forEach(participantes::add);
        return List.copyOf(participantes);
    }

    public void eliminarParticipante(Long planId, Long usuarioId, String emailPropietario) {
        Plan plan = buscarPlanPropietario(planId, emailPropietario);
        if (plan.getUsuario().getId().equals(usuarioId)) {
            throw new ConflictoRecursoException("El propietario no puede eliminarse del plan");
        }
        PlanMember member = planMemberRepository.findByPlanIdAndUsuarioId(planId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la participación solicitada"
                ));
        planMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<PlanInvitationResponse> obtenerInvitacionesPendientes(String email) {
        Usuario usuario = buscarUsuario(email);
        return planMemberRepository.findAllByUsuarioIdAndStatusOrderByInvitedAtDesc(
                        usuario.getId(),
                        PlanMemberStatus.PENDING
                )
                .stream()
                .map(member -> {
                    Plan plan = member.getPlan();
                    return new PlanInvitationResponse(
                            member.getId(),
                            plan.getId(),
                            plan.getTitulo(),
                            plan.getCiudad(),
                            plan.getUsuario() == null ? null : plan.getUsuario().getNombre(),
                            member.getInvitedAt()
                    );
                })
                .toList();
    }

    public void aceptarInvitacion(Long invitationId, String email) {
        resolverInvitacion(invitationId, email, PlanMemberStatus.ACCEPTED);
    }

    public void rechazarInvitacion(Long invitationId, String email) {
        resolverInvitacion(invitationId, email, PlanMemberStatus.REJECTED);
    }

    private void resolverInvitacion(Long invitationId, String email, PlanMemberStatus status) {
        Usuario usuario = buscarUsuario(email);
        PlanMember member = planMemberRepository.findByIdAndUsuarioId(invitationId, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la invitación solicitada"
                ));
        if (member.getStatus() != PlanMemberStatus.PENDING) {
            throw new ConflictoRecursoException("La invitación ya fue resuelta");
        }
        member.setStatus(status);
        member.setJoinedAt(status == PlanMemberStatus.ACCEPTED ? LocalDateTime.now() : null);
        planMemberRepository.save(member);
    }

    private Plan buscarPlanPropietario(Long planId, String email) {
        Usuario propietario = buscarUsuario(email);
        return planRepository.findByIdAndUsuarioId(planId, propietario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el plan"
                ));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario autenticado"));
    }

    private PlanMemberResponse convertirAResponse(PlanMember member) {
        Usuario usuario = member.getUsuario();
        return new PlanMemberResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                member.getStatus(),
                PlanOwnership.MEMBER
        );
    }
}
