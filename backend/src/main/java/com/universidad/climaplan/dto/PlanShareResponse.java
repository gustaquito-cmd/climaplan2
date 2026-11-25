package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estado del enlace público de un plan")
public record PlanShareResponse(
        @Schema(description = "Token del enlace; solo se entrega al propietario autenticado")
        String token,
        @Schema(description = "Indica si el enlace está activo", example = "true")
        boolean active,
        @Schema(description = "Fecha de creación del enlace")
        LocalDateTime createdAt,
        @Schema(description = "Fecha opcional de expiración")
        LocalDateTime expiresAt
) {
}
