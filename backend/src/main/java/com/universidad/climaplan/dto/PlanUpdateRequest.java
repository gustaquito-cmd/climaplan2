package com.universidad.climaplan.dto;

import com.universidad.climaplan.entity.EstadoPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Datos para actualizar un plan")
public record PlanUpdateRequest(
        @NotBlank(message = "El título es obligatorio")
        @Schema(description = "Título del plan", example = "Senderismo", requiredMode = Schema.RequiredMode.REQUIRED)
        String titulo,
        @NotBlank(message = "La ciudad es obligatoria")
        @Schema(description = "Ciudad de destino", example = "Madrid", requiredMode = Schema.RequiredMode.REQUIRED)
        String ciudad,
        @NotNull(message = "La fecha es obligatoria")
        @Schema(description = "Fecha planeada", example = "2026-12-01", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate fecha,
        @NotBlank(message = "El tipo de actividad es obligatorio")
        @Schema(description = "Tipo de actividad", example = "Senderismo", requiredMode = Schema.RequiredMode.REQUIRED)
        String tipoActividad,
        @Schema(description = "Notas adicionales", example = "Llevar protector solar")
        String notas,
        @Schema(description = "Estado del plan", allowableValues = {"PENDIENTE", "COMPLETADO", "CANCELADO"}, example = "PENDIENTE")
        EstadoPlan estado
) {
}
