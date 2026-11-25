package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.PlanMessageResponse;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import com.universidad.climaplan.service.PlanMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/plans/{planId}/messages")
@Tag(name = "Chat de planes", description = "Historial paginado del chat privado de un plan")
@SecurityRequirement(name = "bearerAuth")
public class PlanMessageController {

    private final PlanMessageService planMessageService;

    public PlanMessageController(PlanMessageService planMessageService) {
        this.planMessageService = planMessageService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar el historial del chat",
            description = "Solo el propietario o un participante aceptado puede consultar los mensajes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página cronológica de mensajes",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Paginación inválida",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado o sin acceso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Page<PlanMessageResponse>> historial(
            @PathVariable Long planId,
            @Parameter(description = "Número de página desde cero")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Mensajes por página, máximo 100")
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planMessageService.historial(
                planId,
                authentication.getName(),
                page,
                size
        ));
    }
}
