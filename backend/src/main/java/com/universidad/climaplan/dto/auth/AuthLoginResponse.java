package com.universidad.climaplan.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Resultado de login: sesión iniciada o verificación adicional requerida",
        oneOf = {AuthResponse.class, TwoFactorRequiredResponse.class}
)
public interface AuthLoginResponse {
}
