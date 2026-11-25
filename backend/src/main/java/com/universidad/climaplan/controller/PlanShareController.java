package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.PlanShareResponse;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import com.universidad.climaplan.service.PlanShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans/{planId}/share")
@Tag(name = "Compartir planes", description = "Administración privada de enlaces públicos de planes")
@SecurityRequirement(name = "bearerAuth")
public class PlanShareController {

    private final PlanShareService planShareService;

    public PlanShareController(PlanShareService planShareService) {
        this.planShareService = planShareService;
    }

    @PostMapping
    @Operation(summary = "Generar enlace de un plan", description = "Solo el propietario puede crear o reactivar el enlace.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enlace disponible",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanShareResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PlanShareResponse> crear(
            @PathVariable Long planId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planShareService.crearEnlace(planId, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Consultar enlace del plan", description = "Solo el propietario puede consultar el estado y token activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del enlace",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanShareResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PlanShareResponse> obtener(
            @PathVariable Long planId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planShareService.obtenerEnlace(planId, authentication.getName()));
    }

    @DeleteMapping
    @Operation(summary = "Revocar enlace del plan", description = "Desactiva el enlace público del propietario.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Enlace revocado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> revocar(
            @PathVariable Long planId,
            Authentication authentication
    ) {
        planShareService.revocarEnlace(planId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
