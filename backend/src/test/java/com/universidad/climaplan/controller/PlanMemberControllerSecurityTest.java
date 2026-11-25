package com.universidad.climaplan.controller;

import com.universidad.climaplan.config.SecurityConfig;
import com.universidad.climaplan.dto.PlanInvitationResponse;
import com.universidad.climaplan.dto.PlanMemberResponse;
import com.universidad.climaplan.dto.PlanOwnership;
import com.universidad.climaplan.entity.PlanMemberStatus;
import com.universidad.climaplan.security.JwtService;
import com.universidad.climaplan.service.PlanMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PlanMemberController.class, PlanInvitationController.class})
@Import(SecurityConfig.class)
class PlanMemberControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanMemberService planMemberService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void ownerPuedeInvitarYControllerUsaIdentidadAutenticada() throws Exception {
        when(planMemberService.invitar(
                eq(91L),
                org.mockito.ArgumentMatchers.any(),
                eq("owner@example.com")
        )).thenReturn(new PlanMemberResponse(
                9L,
                "Carlos",
                "carlos@example.com",
                PlanMemberStatus.PENDING,
                PlanOwnership.MEMBER
        ));

        mockMvc.perform(post("/api/v1/plans/91/members")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"carlos@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.estado").value("PENDING"))
                .andExpect(jsonPath("$.rol").value("MEMBER"));

        verify(planMemberService).invitar(
                eq(91L),
                org.mockito.ArgumentMatchers.any(),
                eq("owner@example.com")
        );
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void participantePuedeListarParticipantesYVerInvitacionesPropias() throws Exception {
        when(planMemberService.listarParticipantes(91L, "member@example.com"))
                .thenReturn(List.of(new PlanMemberResponse(
                        9L,
                        "Carlos",
                        "carlos@example.com",
                        PlanMemberStatus.ACCEPTED,
                        PlanOwnership.MEMBER
                )));
        when(planMemberService.obtenerInvitacionesPendientes("member@example.com"))
                .thenReturn(List.of(new PlanInvitationResponse(
                        27L,
                        91L,
                        "Viaje",
                        "San Cristóbal",
                        "Gustavo",
                        LocalDateTime.of(2026, 11, 24, 9, 0)
                )));

        mockMvc.perform(get("/api/v1/plans/91/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rol").value("MEMBER"));
        mockMvc.perform(get("/api/v1/plan-invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tituloPlan").value("Viaje"));

        verify(planMemberService).listarParticipantes(91L, "member@example.com");
        verify(planMemberService).obtenerInvitacionesPendientes("member@example.com");
    }

    @Test
    @WithMockUser(username = "invitee@example.com", roles = "USER")
    void aceptarYRechazarDeleganLaCuentaAutenticada() throws Exception {
        mockMvc.perform(post("/api/v1/plan-invitations/27/accept"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/plan-invitations/28/reject"))
                .andExpect(status().isNoContent());

        verify(planMemberService).aceptarInvitacion(27L, "invitee@example.com");
        verify(planMemberService).rechazarInvitacion(28L, "invitee@example.com");
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void propietarioPuedeEliminarParticipante() throws Exception {
        mockMvc.perform(delete("/api/v1/plans/91/members/9"))
                .andExpect(status().isNoContent());

        verify(planMemberService).eliminarParticipante(91L, 9L, "owner@example.com");
    }

    @Test
    void endpointsColaborativosRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/plan-invitations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/plans/91/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void invitacionValidaEmailAntesDeInvocarServicio() throws Exception {
        mockMvc.perform(post("/api/v1/plans/91/members")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
