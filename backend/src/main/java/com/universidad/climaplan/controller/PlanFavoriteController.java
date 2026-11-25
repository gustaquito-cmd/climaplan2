package com.universidad.climaplan.controller;

import com.universidad.climaplan.service.PlanFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Favoritos de planes")
@SecurityRequirement(name = "bearerAuth")
public class PlanFavoriteController {
    private final PlanFavoriteService service;

    public PlanFavoriteController(PlanFavoriteService service) {
        this.service = service;
    }

    @GetMapping("/favorites")
    @Operation(summary = "Listar IDs de planes favoritos visibles")
    public List<Long> list(Authentication authentication) {
        return service.list(authentication.getName(), isAdmin(authentication));
    }

    @PutMapping("/{id}/favorite")
    @Operation(summary = "Agregar un plan a favoritos")
    public ResponseEntity<Void> add(@PathVariable Long id, Authentication authentication) {
        service.add(id, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "Quitar un plan de favoritos")
    public ResponseEntity<Void> remove(@PathVariable Long id, Authentication authentication) {
        service.remove(id, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
