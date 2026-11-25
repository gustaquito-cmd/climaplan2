package com.universidad.climaplan.dto;

import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.entity.EstadoPlan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Información pública de solo lectura de un plan compartido")
public record SharedPlanResponse(
        @Schema(description = "Título del plan")
        String titulo,
        @Schema(description = "Ciudad de destino")
        String ciudad,
        @Schema(description = "Fecha planeada")
        LocalDate fecha,
        @Schema(description = "Tipo de actividad")
        String tipoActividad,
        @Schema(description = "Estado del plan")
        EstadoPlan estado,
        @Schema(description = "Estado climático actual")
        EstadoClimatico estadoClimatico,
        @Schema(description = "Información meteorológica actual")
        SharedWeatherResponse clima,
        @Schema(description = "Recomendación meteorológica")
        String recomendacion,
        @Schema(description = "Aviso cuando la evaluación climática no está disponible")
        String avisoClimatico
) {
}
