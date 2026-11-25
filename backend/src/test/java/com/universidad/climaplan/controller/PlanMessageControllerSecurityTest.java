package com.universidad.climaplan.controller;

import com.universidad.climaplan.config.SecurityConfig;
import com.universidad.climaplan.dto.PlanMessageResponse;
import com.universidad.climaplan.security.JwtService;
import com.universidad.climaplan.service.PlanMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlanMessageController.class)
@Import(SecurityConfig.class)
class PlanMessageControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanMessageService planMessageService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void obtieneHistorialAutenticadoConPaginacionAcotada() throws Exception {
        when(planMessageService.historial(91L, "member@example.com", 0, 50))
                .thenReturn(new PageImpl<>(
                        List.of(new PlanMessageResponse(
                                501L,
                                91L,
                                9L,
                                "Carlos",
                                "Hola",
                                Instant.parse("2026-11-24T18:00:00Z")
                        )),
                        PageRequest.of(0, 50),
                        1
                ));

        mockMvc.perform(get("/api/v1/plans/91/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].senderName").value("Carlos"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist());

        verify(planMessageService).historial(91L, "member@example.com", 0, 50);
    }

    @Test
    void elHistorialNoEsPublico() throws Exception {
        mockMvc.perform(get("/api/v1/plans/91/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void tamañoDePaginaNoPuedeSuperarElLimite() throws Exception {
        mockMvc.perform(get("/api/v1/plans/91/messages").param("size", "101"))
                .andExpect(status().isBadRequest());
    }
}
