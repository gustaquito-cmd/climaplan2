package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Relación del usuario autenticado con el plan")
public enum PlanOwnership {
    OWNER,
    MEMBER,
    ADMIN
}
