package com.universidad.climaplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear un usuario")
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
        @Pattern(regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$",
                message = "El nombre solo puede contener letras, espacios, apóstrofes y guiones")
        @Schema(description = "Nombre completo", example = "Gustavo Pérez", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 80)
        String nombre,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Schema(description = "Correo electrónico", example = "gustavo@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Schema(description = "Contraseña", example = "ClaveSegura123", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
        String password
) {
}
