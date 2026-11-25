package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "El login requiere validar un código de segundo factor")
public record TwoFactorRequiredResponse(
        @Schema(description = "Indica que se requiere completar 2FA", example = "true")
        boolean twoFactorRequired,
        @Schema(description = "Identificador aleatorio del challenge", example = "b63d8761-dba2-45dc-9f4d-40f3efb1fce2")
        String challengeId,
        @Schema(description = "Estado del envío", example = "Código enviado")
        String message
) implements AuthLoginResponse {
}
