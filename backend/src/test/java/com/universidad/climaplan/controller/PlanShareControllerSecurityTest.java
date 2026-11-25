package com.universidad.climaplan.controller;

import com.universidad.climaplan.config.SecurityConfig;
import com.universidad.climaplan.dto.SharedPlanResponse;
import com.universidad.climaplan.dto.SharedWeatherResponse;
import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.security.JwtService;
import com.universidad.climaplan.service.PlanShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PlanShareController.class, SharedPlanController.class})
@Import(SecurityConfig.class)
class PlanShareControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanShareService planShareService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void endpointPublicoDevuelveSoloElDtoPublicoSinJwt() throws Exception {
        when(planShareService.obtenerPlanCompartido("random-token"))
                .thenReturn(new SharedPlanResponse(
                        "Caminata",
                        "Tuxtla Gutiérrez",
                        LocalDate.of(2026, 12, 1),
                        "Senderismo",
                        EstadoPlan.PENDIENTE,
                        EstadoClimatico.FAVORABLE,
                        new SharedWeatherResponse(25, 25, 50, "cielo claro", 3),
                        "Condiciones favorables",
                        null
                ));

        mockMvc.perform(get("/api/v1/shared/plans/random-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Caminata"))
                .andExpect(jsonPath("$.clima.temperatura").value(25))
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
