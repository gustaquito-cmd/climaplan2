package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.PlanInvitationResponse;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import com.universidad.climaplan.service.PlanMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plan-invitations")
@Tag(name = "Invitaciones a planes", description = "Consulta y respuesta a invitaciones de planes")
@SecurityRequirement(name = "bearerAuth")
public class PlanInvitationController {

    private final PlanMemberService planMemberService;

    public PlanInvitationController(PlanMemberService planMemberService) {
        this.planMemberService = planMemberService;
    }

    @GetMapping
    @Operation(summary = "Listar invitaciones pendientes", description = "Devuelve las invitaciones pendientes del usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitaciones pendientes",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PlanInvitationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<PlanInvitationResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(planMemberService.obtenerInvitacionesPendientes(authentication.getName()));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Aceptar invitación", description = "Solo el invitado puede aceptar una invitación pendiente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitación aceptada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invitación no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invitación ya resuelta",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> aceptar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        planMemberService.aceptarInvitacion(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rechazar invitación", description = "Solo el invitado puede rechazar una invitación pendiente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitación rechazada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invitación no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invitación ya resuelta",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> rechazar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        planMemberService.rechazarInvitacion(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
