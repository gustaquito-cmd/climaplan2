package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.PlanMemberInviteRequest;
import com.universidad.climaplan.dto.PlanMemberResponse;
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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/members")
@Tag(name = "Participantes de planes", description = "Invitaciones y participantes de planes colaborativos")
@SecurityRequirement(name = "bearerAuth")
public class PlanMemberController {

    private final PlanMemberService planMemberService;

    public PlanMemberController(PlanMemberService planMemberService) {
        this.planMemberService = planMemberService;
    }

    @PostMapping
    @Operation(summary = "Invitar a un usuario", description = "Solo el propietario puede invitar una cuenta registrada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitación creada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanMemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan o usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invitación duplicada o auto-invitación",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PlanMemberResponse> invitar(
            @PathVariable Long planId,
            @Valid @RequestBody PlanMemberInviteRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planMemberService.invitar(planId, request, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Listar participantes", description = "El propietario y participantes aceptados pueden consultar los participantes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participantes del plan",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PlanMemberResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado o sin acceso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<PlanMemberResponse>> listar(
            @PathVariable Long planId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planMemberService.listarParticipantes(
                planId,
                authentication.getName()
        ));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Eliminar participante", description = "Solo el propietario puede eliminar una participación.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participante eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan o participante no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El propietario no puede eliminarse a sí mismo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminar(
            @PathVariable Long planId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        planMemberService.eliminarParticipante(planId, userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}
