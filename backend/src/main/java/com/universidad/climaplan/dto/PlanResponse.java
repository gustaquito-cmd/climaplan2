package com.universidad.climaplan.dto;

import com.universidad.climaplan.entity.EstadoPlan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Plan registrado")
public record PlanResponse(
        @Schema(description = "Identificador", example = "1")
        Long id,
        @Schema(description = "Título", example = "Senderismo")
        String titulo,
        @Schema(description = "Ciudad", example = "Madrid")
        String ciudad,
        @Schema(description = "Fecha", example = "2026-12-01")
        LocalDate fecha,
        @Schema(description = "Tipo de actividad", example = "Senderismo")
        String tipoActividad,
        @Schema(description = "Notas", example = "Llevar protector solar")
        String notas,
        @Schema(description = "Estado", allowableValues = {"PENDIENTE", "COMPLETADO", "CANCELADO"}, example = "PENDIENTE")
        EstadoPlan estado,
        @Schema(description = "ID del usuario propietario", example = "1")
        Long usuarioId,
        @Schema(description = "Nombre del propietario", example = "Gustavo Pérez")
        String nombreUsuario,
        @Schema(description = "Relación del usuario autenticado con el plan")
        PlanOwnership ownership
) {
    public PlanResponse(
            Long id,
            String titulo,
            String ciudad,
            LocalDate fecha,
            String tipoActividad,
            String notas,
            EstadoPlan estado,
            Long usuarioId,
            String nombreUsuario
    ) {
        this(id, titulo, ciudad, fecha, tipoActividad, notas, estado, usuarioId, nombreUsuario, null);
    }
}
