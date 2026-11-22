package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar un destino favorito")
public record DestinoFavoritoRequest(
        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        @Schema(description = "Ciudad del destino", example = "Tuxtla Gutiérrez",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        String ciudad,

        @NotBlank(message = "El país es obligatorio")
        @Size(max = 100, message = "El país no puede superar los 100 caracteres")
        @Schema(description = "País del destino", example = "México",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        String pais,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
        @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
        @Schema(description = "Latitud entre -90 y 90", example = "16.7538",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "-90", maximum = "90")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
        @DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
        @Schema(description = "Longitud entre -180 y 180", example = "-93.116",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "-180", maximum = "180")
        Double longitud
) {
}
