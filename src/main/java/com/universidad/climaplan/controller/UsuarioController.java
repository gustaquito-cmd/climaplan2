package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.UsuarioResponse;
import com.universidad.climaplan.dto.UsuarioUpdateRequest;
import com.universidad.climaplan.service.UsuarioService;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "Gestión y consulta de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios; requiere rol administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/users\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/users\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/users\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<List<UsuarioResponse>> obtenerTodos() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario", description = "Obtiene un usuario por identificador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Usuario no encontrado\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar usuario autenticado", description = "Obtiene los datos del usuario vinculado al JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/users/me\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/users/me\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<UsuarioResponse> obtenerUsuarioAutenticado(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.obtenerPorEmail(authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza el nombre y correo de un usuario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"La solicitud contiene campos inválidos\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":{\"nombre\":\"El nombre es obligatorio\"}}"))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Usuario no encontrado\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Conflict", value = "{\"status\":409,\"error\":\"Conflict\",\"message\":\"El email ya está registrado\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar usuario", description = "Desactiva un usuario por identificador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario desactivado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Se requiere autenticación\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para acceder a este recurso\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Not Found", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Usuario no encontrado\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/users/1\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
