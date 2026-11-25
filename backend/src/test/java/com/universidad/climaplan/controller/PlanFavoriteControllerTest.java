package com.universidad.climaplan.controller;

import com.universidad.climaplan.config.SecurityConfig;
import com.universidad.climaplan.security.JwtService;
import com.universidad.climaplan.service.PlanFavoriteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanFavoriteController.class)
@Import(SecurityConfig.class)
class PlanFavoriteControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PlanFavoriteService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@example.com")
    void authenticatedUserCanListAddAndRemoveFavorites() throws Exception {
        when(service.list("user@example.com", false)).thenReturn(List.of(1L, 3L));
        mvc.perform(get("/api/v1/plans/favorites"))
                .andExpect(status().isOk()).andExpect(content().json("[1,3]"));
        mvc.perform(put("/api/v1/plans/1/favorite")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/plans/1/favorite")).andExpect(status().isNoContent());
        verify(service).add(1L, "user@example.com", false);
        verify(service).remove(1L, "user@example.com", false);
    }

    @Test
    void favoritesRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/plans/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/plans/1/favorite")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/plans/1/favorite")).andExpect(status().isUnauthorized());
    }
}
