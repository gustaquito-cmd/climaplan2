package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Contenido de un mensaje nuevo en el chat del plan")
public record PlanMessageRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 1000, message = "El mensaje no puede superar 1000 caracteres")
        @Schema(description = "Texto del mensaje", example = "¿Salimos a las 7?", requiredMode = Schema.RequiredMode.REQUIRED)
        String content
) {
}
