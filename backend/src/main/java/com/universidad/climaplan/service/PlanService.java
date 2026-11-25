package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanCreateRequest;
import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.PlanUpdateRequest;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    public PlanResponse crear(PlanCreateRequest request, String email) {
        Plan plan = new Plan();
        aplicarRequest(plan, request);
        plan.setUsuario(buscarUsuario(email));
        plan.setEstado(EstadoPlan.PENDIENTE);
        return convertirAResponse(planRepository.save(plan));
    }

    public PlanResponse actualizar(Long id, PlanUpdateRequest request, String email, boolean administrador) {
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
