package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.PlanRequest;
import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.weather.PlanWeatherCheckResponse;
import com.universidad.climaplan.service.PlanService;
import com.universidad.climaplan.service.weather.PlanWeatherService;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Planes", description = "Gestión de planes y evaluación meteorológica")
public class PlanController {

    private final PlanService planService;
    private final PlanWeatherService planWeatherService;

    public PlanController(PlanService planService, PlanWeatherService planWeatherService) {
        this.planService = planService;
        this.planWeatherService = planWeatherService;
    }

    @GetMapping
    @Operation(summary = "Listar planes", description = "Lista los planes disponibles según la identidad y el rol del usuario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planes obtenidos",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PlanResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/plans\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/plans\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<List<PlanResponse>> obtenerTodos(Authentication authentication) {
        return ResponseEntity.ok(planService.obtenerTodos(
                authentication.getName(),
                esAdministrador(authentication)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar plan", description = "Obtiene un plan por identificador respetando autorización por propietario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "El plan pertenece a otro usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Plan no encontrado\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<PlanResponse> obtenerPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planService.obtenerPorId(
                id,
                authentication.getName(),
                esAdministrador(authentication)
        ));
    }

    @PostMapping
    @Operation(summary = "Crear plan", description = "Crea un plan asociado al usuario autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plan creado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"La solicitud contiene campos inválidos\",\"path\":\"/api/v1/plans\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":{\"titulo\":\"El título es obligatorio\"}}"))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/plans\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/plans\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<PlanResponse> crear(
            @Valid @RequestBody PlanRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.crear(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar plan", description = "Actualiza un plan respetando autorización por propietario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan actualizado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"La solicitud contiene campos inválidos\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":{\"titulo\":\"El título es obligatorio\"}}"))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "El plan pertenece a otro usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Plan no encontrado\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<PlanResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planService.actualizar(
                id,
                request,
                authentication.getName(),
                esAdministrador(authentication)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar plan", description = "Elimina un plan respetando autorización por propietario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plan eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "El plan pertenece a otro usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Plan no encontrado\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/plans/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        planService.eliminar(id, authentication.getName(), esAdministrador(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/weather-check")
    @Operation(summary = "Evaluar clima de un plan", description = "Consulta OpenWeatherMap y evalúa las condiciones del plan.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación obtenida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanWeatherCheckResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/plans/1/weather-check\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "El plan pertenece a otro usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/plans/1/weather-check\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Plan o ubicación no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Recurso no encontrado\",\"path\":\"/api/v1/plans/1/weather-check\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/plans/1/weather-check\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<PlanWeatherCheckResponse> verificarClima(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planWeatherService.verificarPlan(
                id,
                authentication.getName(),
                esAdministrador(authentication)
        ));
    }

    private boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
