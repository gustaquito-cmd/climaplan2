package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Correo para solicitar el reenvío del código de verificación")
public record ResendVerificationRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Schema(description = "Correo de la cuenta", example = "gustavo@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email
) {
}
