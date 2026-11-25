package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanCreateRequest;
import com.universidad.climaplan.dto.PlanOwnership;
import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.PlanUpdateRequest;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanMember;
import com.universidad.climaplan.entity.PlanMemberStatus;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanMemberRepository;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMemberRepository planMemberRepository;
    private final UsuarioRepository usuarioRepository;

    public PlanService(
            PlanRepository planRepository,
            PlanMemberRepository planMemberRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.planRepository = planRepository;
        this.planMemberRepository = planMemberRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> obtenerTodos(String email, boolean administrador) {
        Usuario usuario = buscarUsuario(email);
        Map<Long, PlanResponse> resultados = new LinkedHashMap<>();
        if (administrador) {
            planRepository.findAll().forEach(plan -> resultados.put(
                    plan.getId(),
                    convertirAResponse(plan, esPropietario(plan, usuario)
                            ? PlanOwnership.OWNER
                            : PlanOwnership.ADMIN)
            ));
            return List.copyOf(resultados.values());
        }

        planRepository.findAllByUsuarioId(usuario.getId()).forEach(plan ->
                resultados.put(plan.getId(), convertirAResponse(plan, PlanOwnership.OWNER)));
        planMemberRepository.findAllByUsuarioIdAndStatusOrderByInvitedAtDesc(
                        usuario.getId(),
                        PlanMemberStatus.ACCEPTED
                )
                .stream()
                .map(PlanMember::getPlan)
                .filter(plan -> !resultados.containsKey(plan.getId()))
                .forEach(plan -> resultados.put(
                        plan.getId(),
                        convertirAResponse(plan, PlanOwnership.MEMBER)
                ));
        return List.copyOf(resultados.values());
    }

    @Transactional(readOnly = true)
    public PlanResponse obtenerPorId(Long id, String email, boolean administrador) {
        if (administrador) {
            Usuario usuario = buscarUsuario(email);
            Plan plan = planRepository.findById(id).orElseThrow(() -> planNoEncontrado(id));
            return convertirAResponse(
                    plan,
                    esPropietario(plan, usuario) ? PlanOwnership.OWNER : PlanOwnership.ADMIN
            );
        }
        Plan plan = obtenerPlanLegible(id, email);
        PlanOwnership ownership = plan.getUsuario() != null
                && email.equals(plan.getUsuario().getEmail())
                ? PlanOwnership.OWNER
                : PlanOwnership.MEMBER;
        return convertirAResponse(plan, ownership);
    }

    @Transactional(readOnly = true)
    public Plan obtenerPlanLegible(Long id, String email) {
        Usuario usuario = buscarUsuario(email);
        return planRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseGet(() -> planMemberRepository.findByPlanIdAndUsuarioId(id, usuario.getId())
                        .filter(member -> member.getStatus() == PlanMemberStatus.ACCEPTED)
                        .map(PlanMember::getPlan)
                        .orElseThrow(() -> planNoEncontrado(id)));
    }

    public PlanResponse crear(PlanCreateRequest request, String email) {
        Plan plan = new Plan();
        aplicarRequest(plan, request);
        plan.setUsuario(buscarUsuario(email));
        plan.setEstado(EstadoPlan.PENDIENTE);
        return convertirAResponse(planRepository.save(plan), PlanOwnership.OWNER);
    }

    public PlanResponse actualizar(Long id, PlanUpdateRequest request, String email, boolean administrador) {
        Plan plan = buscarPlanPropietario(id, email);
        aplicarRequest(plan, request);
        if (request.estado() != null) {
            plan.setEstado(request.estado());
        }
        return convertirAResponse(planRepository.save(plan), PlanOwnership.OWNER);
    }

    public void eliminar(Long id, String email, boolean administrador) {
        Plan plan = buscarPlanPropietario(id, email);
        planRepository.delete(plan);
    }

    private Plan buscarPlanPropietario(Long id, String email) {
        Usuario usuario = buscarUsuario(email);
        return planRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el plan con ID " + id
                ));
    }

    private RecursoNoEncontradoException planNoEncontrado(Long id) {
        return new RecursoNoEncontradoException("No se encontró el plan con ID " + id);
    }

    private boolean esPropietario(Plan plan, Usuario usuario) {
        return plan.getUsuario() != null && plan.getUsuario().getId().equals(usuario.getId());
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario autenticado"
                ));
    }

    private void aplicarRequest(Plan plan, PlanCreateRequest request) {
        aplicarCampos(plan, request.titulo(), request.ciudad(), request.fecha(),
                request.tipoActividad(), request.notas());
    }

    private void aplicarRequest(Plan plan, PlanUpdateRequest request) {
        aplicarCampos(plan, request.titulo(), request.ciudad(), request.fecha(),
                request.tipoActividad(), request.notas());
    }

    private void aplicarCampos(
            Plan plan,
            String titulo,
            String ciudad,
            LocalDate fecha,
            String tipoActividad,
            String notas
    ) {
        plan.setTitulo(titulo);
        plan.setCiudad(ciudad);
        plan.setFecha(fecha);
        plan.setTipoActividad(tipoActividad);
        plan.setNotas(notas);
    }

    private PlanResponse convertirAResponse(Plan plan, PlanOwnership ownership) {
        return new PlanResponse(
                plan.getId(),
                plan.getTitulo(),
                plan.getCiudad(),
                plan.getFecha(),
                plan.getTipoActividad(),
                plan.getNotas(),
                plan.getEstado(),
                plan.getUsuario() == null ? null : plan.getUsuario().getId(),
                plan.getUsuario() == null ? null : plan.getUsuario().getNombre(),
                ownership
        );
    }
}
