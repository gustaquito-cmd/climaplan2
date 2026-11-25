package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Invitación pendiente a un plan")
public record PlanInvitationResponse(
        @Schema(description = "ID de la invitación")
        Long id,
        @Schema(description = "ID del plan")
        Long planId,
        @Schema(description = "Título del plan")
        String tituloPlan,
        @Schema(description = "Ciudad del plan")
        String ciudad,
        @Schema(description = "Nombre del propietario")
        String propietarioNombre,
        @Schema(description = "Fecha de envío de la invitación")
        LocalDateTime invitedAt
) {
}
