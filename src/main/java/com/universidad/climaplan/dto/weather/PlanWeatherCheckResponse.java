package com.universidad.climaplan.dto.weather;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Evaluación meteorológica asociada a un plan")
public record PlanWeatherCheckResponse(
        @Schema(description = "ID del plan", example = "1")
        Long planId,
        @Schema(description = "Título del plan", example = "Senderismo")
        String titulo,
        @Schema(description = "Ciudad", example = "Madrid")
        String ciudad,
        @Schema(description = "Fecha", example = "2026-12-01")
        LocalDate fecha,
        @Schema(description = "Estado climático", allowableValues = {"FAVORABLE", "PRECAUCION", "NO_RECOMENDABLE"})
        EstadoClimatico estadoClimatico,
        @Schema(description = "Datos meteorológicos")
        WeatherResponse clima,
        @Schema(description = "Recomendación", example = "Las condiciones son favorables")
        String recomendacion
) {
}
