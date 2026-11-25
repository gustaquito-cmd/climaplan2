package com.universidad.climaplan.dto;

import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.dto.weather.WeatherDataType;
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
        @Schema(description = "Tipo de datos meteorológicos disponibles")
        WeatherDataType weatherDataType,
        @Schema(description = "Estado climático asociado a los datos disponibles")
        EstadoClimatico estadoClimatico,
        @Schema(description = "Información meteorológica; vacío cuando no hay datos para la fecha")
        SharedWeatherResponse clima,
        @Schema(description = "Recomendación meteorológica")
        String recomendacion,
        @Schema(description = "Aviso cuando no hay datos meteorológicos o el proveedor no responde")
        String avisoClimatico
) {
}
