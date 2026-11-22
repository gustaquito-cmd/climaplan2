package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Schema(description = "Correo registrado", example = "gustavo@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Schema(description = "Contraseña del usuario", example = "ClaveSegura123", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {
}
