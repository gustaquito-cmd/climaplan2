package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "Código OTP asociado al challenge de segundo factor")
public record VerifyTwoFactorRequest(
        @NotNull(message = "El challengeId es obligatorio")
        @Schema(description = "UUID recibido al iniciar sesión", requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
        UUID challengeId,
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código debe contener 6 dígitos")
        @Schema(description = "Código OTP de seis dígitos", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {
}
