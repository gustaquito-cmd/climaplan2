package com.universidad.climaplan.dto;

import com.universidad.climaplan.entity.PlanMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Información no sensible de una persona asociada al plan")
public record PlanMemberResponse(
        @Schema(description = "ID del usuario")
        Long id,
        @Schema(description = "Nombre")
        String nombre,
        @Schema(description = "Email")
        String email,
        @Schema(description = "Estado de invitación")
        PlanMemberStatus estado,
        @Schema(description = "Rol derivado de la propiedad o participación")
        PlanOwnership rol
) {
}
