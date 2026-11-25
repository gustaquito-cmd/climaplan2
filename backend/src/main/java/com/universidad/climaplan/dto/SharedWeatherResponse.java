package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos meteorológicos limitados para mostrar en un plan público")
public record SharedWeatherResponse(
        @Schema(description = "Temperatura en grados Celsius")
        double temperatura,
        @Schema(description = "Sensación térmica en grados Celsius")
        double sensacionTermica,
        @Schema(description = "Humedad porcentual")
        int humedad,
        @Schema(description = "Descripción meteorológica")
        String descripcion,
        @Schema(description = "Velocidad del viento en m/s")
        double velocidadViento
) {
}
