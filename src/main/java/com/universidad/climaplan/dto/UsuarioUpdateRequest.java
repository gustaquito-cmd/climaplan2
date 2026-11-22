package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Datos editables del usuario")
public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Schema(description = "Nombre completo", example = "Gustavo Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
        String nombre,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Schema(description = "Correo electrónico", example = "gustavo@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email
) {
}
