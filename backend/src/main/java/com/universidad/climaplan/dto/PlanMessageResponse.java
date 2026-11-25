package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Mensaje de chat de un plan")
public record PlanMessageResponse(
        @Schema(description = "ID del mensaje")
        Long id,
        @Schema(description = "ID del plan")
        Long planId,
        @Schema(description = "ID del usuario remitente")
        Long senderId,
        @Schema(description = "Nombre del remitente")
        String senderName,
        @Schema(description = "Contenido de texto")
        String content,
        @Schema(description = "Instante UTC de envío")
        Instant sentAt
) {
}
