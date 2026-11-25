package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de una operación de verificación de correo")
public record AuthOperationResponse(
        @Schema(description = "Mensaje de resultado", example = "Revisa tu correo para continuar.")
        String message
) {
}
