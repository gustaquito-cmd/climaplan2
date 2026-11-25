package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Correo y código de un solo uso para verificar la cuenta")
public record VerifyEmailRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Schema(description = "Correo de la cuenta", example = "gustavo@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código debe contener 6 dígitos")
        @Schema(description = "Código de verificación de 6 dígitos", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {
}
