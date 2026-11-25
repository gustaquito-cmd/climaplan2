package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanShareResponse;
import com.universidad.climaplan.dto.SharedPlanResponse;
import com.universidad.climaplan.dto.SharedWeatherResponse;
import com.universidad.climaplan.dto.weather.PlanWeatherCheckResponse;
import com.universidad.climaplan.dto.weather.WeatherDataType;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanShare;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.OpenWeatherException;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.PlanShareRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.service.weather.PlanWeatherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class PlanShareService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final PlanRepository planRepository;
    private final PlanShareRepository planShareRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanWeatherService planWeatherService;

    public PlanShareService(
            PlanRepository planRepository,
            PlanShareRepository planShareRepository,
            UsuarioRepository usuarioRepository,
            PlanWeatherService planWeatherService
    ) {
        this.planRepository = planRepository;
        this.planShareRepository = planShareRepository;
        this.usuarioRepository = usuarioRepository;
        this.planWeatherService = planWeatherService;
    }

    public PlanShareResponse crearEnlace(Long planId, String email) {
        Plan plan = buscarPlanPropietario(planId, email);
        PlanShare share = planShareRepository.findByPlanId(planId).orElseGet(PlanShare::new);

        if (enlaceActivo(share)) {
            return convertirAResponse(share);
        }

        share.setPlan(plan);
        share.setToken(generarToken());
        share.setActive(true);
        share.setCreatedAt(LocalDateTime.now());
        share.setExpiresAt(null);
        return convertirAResponse(planShareRepository.save(share));
    }

    @Transactional(readOnly = true)
    public PlanShareResponse obtenerEnlace(Long planId, String email) {
        buscarPlanPropietario(planId, email);
        Optional<PlanShare> share = planShareRepository.findByPlanId(planId)
                .filter(this::enlaceActivo);
        return share.map(this::convertirAResponse)
                .orElseGet(() -> new PlanShareResponse(null, false, null, null));
    }

    public void revocarEnlace(Long planId, String email) {
        buscarPlanPropietario(planId, email);
        planShareRepository.findByPlanId(planId).ifPresent(share -> {
            if (share.isActive()) {
                share.setActive(false);
                planShareRepository.save(share);
            }
        });
    }

    @Transactional(readOnly = true)
    public SharedPlanResponse obtenerPlanCompartido(String token) {
        PlanShare share = planShareRepository.findByTokenAndActiveTrue(token)
                .filter(this::enlaceActivo)
                .orElseThrow(this::enlaceNoDisponible);
        Plan plan = share.getPlan();
        PlanWeatherCheckResponse weather;
        try {
            weather = planWeatherService.verificarPlanCompartido(plan);
        } catch (OpenWeatherException exception) {
            return new SharedPlanResponse(
                    plan.getTitulo(),
                    plan.getCiudad(),
                    plan.getFecha(),
                    plan.getTipoActividad(),
                    plan.getEstado(),
                    WeatherDataType.SERVICE_ERROR,
                    null,
                    null,
                    null,
                    "La evaluación climática no está disponible en este momento."
            );
        }

        return new SharedPlanResponse(
                plan.getTitulo(),
                plan.getCiudad(),
                plan.getFecha(),
                plan.getTipoActividad(),
                plan.getEstado(),
                weather.weatherDataType(),
                weather.estadoClimatico(),
                weather.clima() == null ? null : new SharedWeatherResponse(
                        weather.clima().temperatura(),
                        weather.clima().sensacionTermica(),
                        weather.clima().humedad(),
                        weather.clima().descripcion(),
                        weather.clima().velocidadViento()
                ),
                weather.recomendacion(),
                weather.clima() == null ? weather.mensaje() : null
        );
    }

    private Plan buscarPlanPropietario(Long planId, String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el plan"
                ));
        return planRepository.findByIdAndUsuarioId(planId, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el plan"
                ));
    }

    private boolean enlaceActivo(PlanShare share) {
        return share.isActive()
                && (share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private PlanShareResponse convertirAResponse(PlanShare share) {
        return new PlanShareResponse(
                share.getToken(),
                share.isActive(),
                share.getCreatedAt(),
                share.getExpiresAt()
        );
    }

    private String generarToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    private RecursoNoEncontradoException enlaceNoDisponible() {
        return new RecursoNoEncontradoException("Este enlace ya no está disponible");
    }
}
