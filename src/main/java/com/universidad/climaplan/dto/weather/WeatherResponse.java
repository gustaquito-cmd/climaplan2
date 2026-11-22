package com.universidad.climaplan.dto.weather;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Información meteorológica actual")
public record WeatherResponse(
        @Schema(description = "Ciudad", example = "Madrid")
        String ciudad,
        @Schema(description = "Código de país", example = "ES")
        String pais,
        @Schema(description = "Latitud", example = "40.4165")
        double lat,
        @Schema(description = "Longitud", example = "-3.70256")
        double lon,
        @Schema(description = "Temperatura en grados Celsius", example = "21.5")
        double temperatura,
        @Schema(description = "Sensación térmica", example = "20.8")
        double sensacionTermica,
        @Schema(description = "Humedad porcentual", example = "45")
        int humedad,
        @Schema(description = "Descripción meteorológica", example = "cielo despejado")
        String descripcion,
        @Schema(description = "Velocidad del viento", example = "3.2")
        double velocidadViento
) {
}
