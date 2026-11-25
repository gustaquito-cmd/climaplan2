package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Datos para invitar un usuario registrado a un plan")
public record PlanMemberInviteRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Schema(description = "Email de una cuenta registrada", example = "usuario@correo.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email
) {
}
