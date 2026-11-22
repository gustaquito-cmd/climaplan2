package com.universidad.climaplan.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Respuesta estandarizada para errores HTTP")
public record ApiErrorResponse(
        @Schema(description = "Código de estado HTTP", example = "0")
        int status,

        @Schema(description = "Descripción breve del error", example = "HTTP error")
        String error,

        @Schema(description = "Mensaje descriptivo del error", example = "Descripción del error")
        String message,

        @Schema(description = "Ruta solicitada", example = "/api/v1/resource")
        String path,

        @Schema(description = "Fecha y hora del error", example = "2026-11-20T12:00:00")
        LocalDateTime timestamp,

        @Schema(description = "Errores de validación por campo", example = "{\"campo\":\"mensaje de validación\"}")
        Map<String, String> validationErrors
) {
}
