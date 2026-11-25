package com.universidad.climaplan.controller;

import com.universidad.climaplan.config.SecurityConfig;
import com.universidad.climaplan.dto.PlanCreateRequest;
import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.SharedPlanResponse;
import com.universidad.climaplan.dto.SharedWeatherResponse;
import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.dto.weather.WeatherDataType;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.security.JwtService;
import com.universidad.climaplan.service.PlanService;
import com.universidad.climaplan.service.PlanShareService;
import com.universidad.climaplan.service.weather.PlanWeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PlanController.class, PlanShareController.class, SharedPlanController.class})
@Import(SecurityConfig.class)
class PlanShareControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanShareService planShareService;

    @MockitoBean
    private PlanService planService;

    @MockitoBean
    private PlanWeatherService planWeatherService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "ana@example.com", roles = "USER")
    void postPlanIgnoraEstadoEnviadoYRespondePendiente() throws Exception {
        when(planService.crear(any(PlanCreateRequest.class), eq("ana@example.com")))
                .thenReturn(new PlanResponse(
                        92L,
                        "Caminata",
                        "Tuxtla Gutiérrez",
                        LocalDate.of(2026, 12, 1),
                        "Senderismo",
                        null,
                        EstadoPlan.PENDIENTE,
                        7L,
                        "Ana"
                ));

        mockMvc.perform(post("/api/v1/plans")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Caminata",
                                  "ciudad": "Tuxtla Gutiérrez",
                                  "fecha": "2026-12-01",
                                  "tipoActividad": "Senderismo",
                                  "estado": "COMPLETADO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(planService).crear(any(PlanCreateRequest.class), eq("ana@example.com"));
    }

    @Test
    void endpointPublicoDevuelveSoloElDtoPublicoSinJwt() throws Exception {
        when(planShareService.obtenerPlanCompartido("random-token"))
                .thenReturn(new SharedPlanResponse(
                        "Caminata",
                        "Tuxtla Gutiérrez",
                        LocalDate.of(2026, 12, 1),
                        "Senderismo",
                        EstadoPlan.PENDIENTE,
                        WeatherDataType.FORECAST,
                        EstadoClimatico.FAVORABLE,
                        new SharedWeatherResponse(25, 25, 50, "cielo claro", 3),
                        "Condiciones favorables",
                        null
                ));

        mockMvc.perform(get("/api/v1/shared/plans/random-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Caminata"))
                .andExpect(jsonPath("$.weatherDataType").value("FORECAST"))
                .andExpect(jsonPath("$.clima.temperatura").value(25))
                .andExpect(jsonPath("$.messages").doesNotExist())
                .andExpect(jsonPath("$.usuarioId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.rol").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.clima.lat").doesNotExist())
                .andExpect(jsonPath("$.clima.lon").doesNotExist());

        verify(planShareService).obtenerPlanCompartido("random-token");
    }

    @Test
    void operacionesDeAdministracionDeEnlaceNoSonPublicas() throws Exception {
        mockMvc.perform(post("/api/v1/plans/91/share"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointPublicoNoPermiteOtrosMetodosYDevuelve404ParaTokenInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/shared/plans/random-token"))
                .andExpect(status().is4xxClientError());

        when(planShareService.obtenerPlanCompartido("invalid-token"))
                .thenThrow(new RecursoNoEncontradoException("Este enlace ya no está disponible"));

        mockMvc.perform(get("/api/v1/shared/plans/invalid-token"))
                .andExpect(status().isNotFound());
    }
}
