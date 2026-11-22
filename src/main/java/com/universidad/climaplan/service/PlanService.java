package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanRequest;
import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlanService {

    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;

    public PlanService(PlanRepository planRepository, UsuarioRepository usuarioRepository) {
        this.planRepository = planRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> obtenerTodos(String email, boolean administrador) {
        List<Plan> planes = administrador
                ? planRepository.findAll()
                : planRepository.findAllByUsuarioId(buscarUsuario(email).getId());
        return planes.stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse obtenerPorId(Long id, String email, boolean administrador) {
        return convertirAResponse(buscarPlan(id, email, administrador));
    }

    public PlanResponse crear(PlanRequest request, String email) {
        Plan plan = new Plan();
        aplicarRequest(plan, request);
        plan.setUsuario(buscarUsuario(email));
        plan.setEstado(request.estado() == null ? EstadoPlan.PENDIENTE : request.estado());
        return convertirAResponse(planRepository.save(plan));
    }

    public PlanResponse actualizar(Long id, PlanRequest request, String email, boolean administrador) {
        Plan plan = buscarPlan(id, email, administrador);
        aplicarRequest(plan, request);
        if (request.estado() != null) {
            plan.setEstado(request.estado());
        }
        return convertirAResponse(planRepository.save(plan));
    }

    public void eliminar(Long id, String email, boolean administrador) {
        Plan plan = buscarPlan(id, email, administrador);
        planRepository.delete(plan);
    }

    private Plan buscarPlan(Long id, String email, boolean administrador) {
        return (administrador
                ? planRepository.findById(id)
                : planRepository.findByIdAndUsuarioId(id, buscarUsuario(email).getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el plan con ID " + id
                ));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario autenticado"
                ));
    }

    private void aplicarRequest(Plan plan, PlanRequest request) {
        plan.setTitulo(request.titulo());
        plan.setCiudad(request.ciudad());
        plan.setFecha(request.fecha());
        plan.setTipoActividad(request.tipoActividad());
        plan.setNotas(request.notas());
    }

    private PlanResponse convertirAResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getTitulo(),
                plan.getCiudad(),
                plan.getFecha(),
                plan.getTipoActividad(),
                plan.getNotas(),
                plan.getEstado(),
                plan.getUsuario() == null ? null : plan.getUsuario().getId(),
                plan.getUsuario() == null ? null : plan.getUsuario().getNombre()
        );
    }
}
