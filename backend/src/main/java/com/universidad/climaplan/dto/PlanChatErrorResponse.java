package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error no sensible del chat")
public record PlanChatErrorResponse(
        @Schema(description = "Mensaje legible para el usuario")
        String message
) {
}
