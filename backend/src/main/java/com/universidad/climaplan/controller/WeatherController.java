package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.service.weather.OpenWeatherService;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Clima", description = "Consulta de información meteorológica mediante OpenWeatherMap")
public class WeatherController {

    private final OpenWeatherService openWeatherService;

    public WeatherController(OpenWeatherService openWeatherService) {
        this.openWeatherService = openWeatherService;
    }

    @GetMapping
    @Operation(summary = "Consultar clima actual", description = "Consulta el clima actual de una ciudad mediante OpenWeatherMap.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clima obtenido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = WeatherResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ciudad no especificada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"El parámetro city es obligatorio\",\"path\":\"/api/v1/weather\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/weather\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Ciudad no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Ciudad no encontrada\",\"path\":\"/api/v1/weather\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/weather\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<WeatherResponse> obtenerClima(
            @Parameter(description = "Nombre de la ciudad", required = true, example = "Madrid")
            @RequestParam String city
    ) {
        return ResponseEntity.ok(openWeatherService.consultarClima(city));
    }
}
