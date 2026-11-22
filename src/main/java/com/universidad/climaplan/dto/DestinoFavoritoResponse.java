package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Destino favorito registrado")
public record DestinoFavoritoResponse(
        @Schema(description = "Identificador", example = "1")
        Long id,

        @Schema(description = "Ciudad", example = "Tuxtla Gutiérrez")
        String ciudad,

        @Schema(description = "País", example = "México")
        String pais,

        @Schema(description = "Latitud", example = "16.7538")
        Double latitud,

        @Schema(description = "Longitud", example = "-93.116")
        Double longitud,

        @Schema(description = "Identificador del usuario propietario", example = "1")
        Long usuarioId
) {
}
