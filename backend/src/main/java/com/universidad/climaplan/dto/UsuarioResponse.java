package com.universidad.climaplan.dto;

import com.universidad.climaplan.entity.EstadoUsuario;
import com.universidad.climaplan.entity.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos públicos del usuario")
public record UsuarioResponse(
        @Schema(description = "Identificador", example = "1")
        Long id,
        @Schema(description = "Nombre completo", example = "Gustavo Pérez")
        String nombre,
        @Schema(description = "Correo electrónico", example = "gustavo@example.com")
        String email,
        @Schema(description = "Rol", allowableValues = {"ROLE_USER", "ROLE_ADMIN"}, example = "ROLE_USER")
        Rol rol,
        @Schema(description = "Estado", allowableValues = {"ACTIVO", "INACTIVO"}, example = "ACTIVO")
        EstadoUsuario estado
) {
}
