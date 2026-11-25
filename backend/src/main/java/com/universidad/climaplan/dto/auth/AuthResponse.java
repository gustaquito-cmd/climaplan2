package com.universidad.climaplan.dto.auth;

import com.universidad.climaplan.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de autenticación con token JWT")
public record AuthResponse(
        @Schema(description = "Token JWT de acceso", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,
        @Schema(description = "Duración del token en milisegundos", example = "3600000")
        long expiresIn,
        @Schema(description = "Datos del usuario autenticado")
        UsuarioResponse usuario
) implements AuthLoginResponse {
}
