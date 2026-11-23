package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para registrar un usuario")
public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Schema(description = "Nombre completo", example = "Gustavo Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
        String nombre,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Schema(description = "Correo electrónico", example = "gustavo@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Schema(description = "Contraseña de al menos 8 caracteres", example = "ClaveSegura123", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
        String password
) {
}
