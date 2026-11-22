package com.universidad.climaplan.dto.weather;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado derivado de la evaluación climática")
public enum EstadoClimatico {
    FAVORABLE,
    PRECAUCION,
    NO_RECOMENDABLE
}
