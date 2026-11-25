package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.SharedWeatherResponse;
import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.dto.weather.PlanWeatherCheckResponse;
import com.universidad.climaplan.dto.weather.WeatherDataType;
import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanShare;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.OpenWeatherException;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.PlanShareRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.service.weather.PlanWeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanShareServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanShareRepository planShareRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlanWeatherService planWeatherService;

    @InjectMocks
    private PlanShareService planShareService;

    private Usuario propietario;
    private Plan plan;

    @BeforeEach
    void prepararPropietarioYPlan() {
        propietario = new Usuario();
        ReflectionTestUtils.setField(propietario, "id", 7L);
        plan = new Plan();
        ReflectionTestUtils.setField(plan, "id", 91L);
        plan.setTitulo("Caminata");
        plan.setCiudad("Tuxtla Gutiérrez");
        plan.setFecha(LocalDate.of(2026, 12, 1));
        plan.setTipoActividad("Senderismo");
        plan.setEstado(EstadoPlan.PENDIENTE);

    }

    @Test
    void propietarioGeneraTokenAleatorioSeguroYPuedeConsultarElEnlace() {
        configurarPropietario();
        PlanShare[] savedShare = {null};
        when(planShareRepository.findByPlanId(91L))
                .thenAnswer(invocation -> Optional.ofNullable(savedShare[0]));
        when(planShareRepository.save(any(PlanShare.class)))
                .thenAnswer(invocation -> {
                    savedShare[0] = invocation.getArgument(0);
                    return savedShare[0];
                });

        var created = planShareService.crearEnlace(91L, "ana@example.com");
        var current = planShareService.obtenerEnlace(91L, "ana@example.com");

        assertTrue(created.active());
        assertNotNull(created.createdAt());
        assertEquals(43, created.token().length());
        assertTrue(created.token().matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals("91", created.token());
        assertEquals(created, current);
    }

    @Test
    void usuarioDistintoNoPuedeCompartirPlanYRecibe404() {
        Usuario otroUsuario = new Usuario();
        ReflectionTestUtils.setField(otroUsuario, "id", 12L);
        when(usuarioRepository.findByEmail("otra@example.com"))
                .thenReturn(Optional.of(otroUsuario));
        when(planRepository.findByIdAndUsuarioId(91L, 12L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planShareService.crearEnlace(91L, "otra@example.com")
        );

        verify(planShareRepository, never()).save(any(PlanShare.class));
    }

    @Test
    void propietarioPuedeRevocarYElTokenDejaDeSerAccesible() {
        configurarPropietario();
        PlanShare share = activeShare("secret-token");
        when(planShareRepository.findByPlanId(91L)).thenReturn(Optional.of(share));
        when(planShareRepository.findByTokenAndActiveTrue("secret-token"))
                .thenReturn(Optional.empty());
        when(planShareRepository.save(any(PlanShare.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        planShareService.revocarEnlace(91L, "ana@example.com");
        var state = planShareService.obtenerEnlace(91L, "ana@example.com");

        assertFalse(share.isActive());
        assertFalse(state.active());
        assertNull(state.token());
        verify(planShareRepository).save(share);
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planShareService.obtenerPlanCompartido("secret-token")
        );

        String replacementToken = planShareService.crearEnlace(91L, "ana@example.com").token();
        assertNotEquals("secret-token", replacementToken);
        assertTrue(share.isActive());
    }

    @Test
    void tokenValidoDevuelveSoloDatosPublicosYReutilizaLaEvaluacionClimatica() {
        PlanShare share = activeShare("secure-token");
        WeatherResponse weather = new WeatherResponse(
                "Tuxtla Gutiérrez", "MX", 16.75, -93.12, 25, 25, 50,
                "cielo claro", 3
        );
        when(planShareRepository.findByTokenAndActiveTrue("secure-token"))
                .thenReturn(Optional.of(share));
        when(planWeatherService.verificarPlanCompartido(plan))
                .thenReturn(new PlanWeatherCheckResponse(
                        91L,
                        "Caminata",
                        "Tuxtla Gutiérrez",
                        plan.getFecha(),
                        WeatherDataType.FORECAST,
                        EstadoClimatico.FAVORABLE,
                        weather,
                        "Condiciones pronosticadas favorables",
                        "Pronóstico para " + plan.getFecha()
                ));

        var response = planShareService.obtenerPlanCompartido("secure-token");

        assertEquals("Caminata", response.titulo());
        assertEquals("Tuxtla Gutiérrez", response.ciudad());
        assertEquals("Senderismo", response.tipoActividad());
        assertEquals(WeatherDataType.FORECAST, response.weatherDataType());
        assertEquals(EstadoClimatico.FAVORABLE, response.estadoClimatico());
        assertEquals(new SharedWeatherResponse(25, 25, 50, "cielo claro", 3), response.clima());
        assertEquals("Condiciones pronosticadas favorables", response.recomendacion());
        assertNull(response.avisoClimatico());
        assertEquals(10, response.getClass().getRecordComponents().length);
        verify(planWeatherService).verificarPlanCompartido(plan);
    }

    @Test
    void planCompartidoSigueDisponibleSiElServicioClimaticoNoResponde() {
        when(planShareRepository.findByTokenAndActiveTrue("secure-token"))
                .thenReturn(Optional.of(activeShare("secure-token")));
        when(planWeatherService.verificarPlanCompartido(plan))
                .thenThrow(new OpenWeatherException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Proveedor meteorológico no disponible"
                ));

        var response = planShareService.obtenerPlanCompartido("secure-token");

        assertEquals("Caminata", response.titulo());
        assertEquals(WeatherDataType.SERVICE_ERROR, response.weatherDataType());
        assertNull(response.clima());
        assertEquals(
                "La evaluación climática no está disponible en este momento.",
                response.avisoClimatico()
        );
    }

    @Test
    void planCompartidoConPronosticoNoDisponibleNoMuestraClimaActual() {
        when(planShareRepository.findByTokenAndActiveTrue("secure-token"))
                .thenReturn(Optional.of(activeShare("secure-token")));
        when(planWeatherService.verificarPlanCompartido(plan))
                .thenReturn(new PlanWeatherCheckResponse(
                        91L,
                        "Caminata",
                        "Tuxtla Gutiérrez",
                        plan.getFecha(),
                        WeatherDataType.NOT_AVAILABLE,
                        null,
                        null,
                        null,
                        "El pronóstico todavía no está disponible para esta fecha."
                ));

        var response = planShareService.obtenerPlanCompartido("secure-token");

        assertEquals(WeatherDataType.NOT_AVAILABLE, response.weatherDataType());
        assertNull(response.clima());
        assertNull(response.estadoClimatico());
        assertEquals(
                "El pronóstico todavía no está disponible para esta fecha.",
                response.avisoClimatico()
        );
        verify(planWeatherService).verificarPlanCompartido(plan);
    }

    @Test
    void tokenInexistenteYTokenExpiradoNoExponenElPlan() {
        when(planShareRepository.findByTokenAndActiveTrue("unknown-token"))
                .thenReturn(Optional.empty());
        PlanShare expired = activeShare("expired-token");
        expired.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        when(planShareRepository.findByTokenAndActiveTrue("expired-token"))
                .thenReturn(Optional.of(expired));

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planShareService.obtenerPlanCompartido("unknown-token")
        );
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planShareService.obtenerPlanCompartido("expired-token")
        );
    }

    private PlanShare activeShare(String token) {
        PlanShare share = new PlanShare();
        share.setPlan(plan);
        share.setToken(token);
        share.setActive(true);
        share.setCreatedAt(java.time.LocalDateTime.now());
        return share;
    }

    private void configurarPropietario() {
        when(usuarioRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(propietario));
        when(planRepository.findByIdAndUsuarioId(91L, 7L))
                .thenReturn(Optional.of(plan));
    }
}
