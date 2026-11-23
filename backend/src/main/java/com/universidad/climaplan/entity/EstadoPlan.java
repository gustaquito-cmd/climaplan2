package com.universidad.climaplan.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de ejecución de un plan")
public enum EstadoPlan {
    PENDIENTE,
    COMPLETADO,
    CANCELADO
}
