package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.SharedPlanResponse;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import com.universidad.climaplan.service.PlanShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shared/plans")
@Tag(name = "Planes compartidos", description = "Consulta pública y de solo lectura de planes")
public class SharedPlanController {

    private final PlanShareService planShareService;

    public SharedPlanController(PlanShareService planShareService) {
        this.planShareService = planShareService;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Consultar plan compartido", description = "Endpoint público de solo lectura; no requiere autenticación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan público obtenido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SharedPlanResponse.class))),
            @ApiResponse(responseCode = "404", description = "Enlace inexistente, revocado o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "No fue posible evaluar el clima",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SharedPlanResponse> obtener(@PathVariable String token) {
        return ResponseEntity.ok(planShareService.obtenerPlanCompartido(token));
    }
}
