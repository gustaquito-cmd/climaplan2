package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanMessageRequest;
import com.universidad.climaplan.dto.PlanMessageResponse;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanMessage;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanMessageRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

@Service
@Transactional
public class PlanMessageService {

    public static final int MAX_CONTENT_LENGTH = 1000;
    public static final int MAX_PAGE_SIZE = 100;

    private final PlanMessageRepository planMessageRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanService planService;

    public PlanMessageService(
            PlanMessageRepository planMessageRepository,
            UsuarioRepository usuarioRepository,
            PlanService planService
    ) {
        this.planMessageRepository = planMessageRepository;
        this.usuarioRepository = usuarioRepository;
        this.planService = planService;
    }

    @Transactional(readOnly = true)
    public Page<PlanMessageResponse> historial(Long planId, String email, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("La paginación solicitada no es válida");
        }
        planService.obtenerPlanLegible(planId, email);
        Page<PlanMessage> newestFirst = planMessageRepository.findAllByPlanIdOrderBySentAtDescIdDesc(
                planId,
                PageRequest.of(page, size)
        );
        var chronological = new ArrayList<>(newestFirst.getContent().stream()
                .map(this::convertirAResponse)
                .toList());
        Collections.reverse(chronological);
        return new PageImpl<>(chronological, newestFirst.getPageable(), newestFirst.getTotalElements());
    }

    public PlanMessageResponse enviar(Long planId, PlanMessageRequest request, String email) {
        Plan plan = planService.obtenerPlanLegible(planId, email);
        String content = request == null || request.content() == null
                ? ""
                : request.content().trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("El mensaje no puede superar 1000 caracteres");
        }

        Usuario sender = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario autenticado"
                ));
        PlanMessage message = new PlanMessage();
        message.setPlan(plan);
        message.setUsuario(sender);
        message.setContent(content);
        message.setSentAt(Instant.now());
        return convertirAResponse(planMessageRepository.save(message));
    }

    private PlanMessageResponse convertirAResponse(PlanMessage message) {
        return new PlanMessageResponse(
                message.getId(),
                message.getPlan().getId(),
                message.getUsuario().getId(),
                message.getUsuario().getNombre(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
