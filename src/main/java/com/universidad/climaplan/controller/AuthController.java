package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.auth.AuthResponse;
import com.universidad.climaplan.dto.auth.LoginRequest;
import com.universidad.climaplan.dto.auth.RegisterRequest;
import com.universidad.climaplan.service.AuthService;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Registro e inicio de sesión mediante JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea una cuenta y devuelve un token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"La solicitud contiene campos inválidos\",\"path\":\"/api/v1/auth/register\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":{\"email\":\"El email debe ser válido\"}}"))),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Conflict", value = "{\"status\":409,\"error\":\"Conflict\",\"message\":\"El email ya está registrado\",\"path\":\"/api/v1/auth/register\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/auth/register\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"La solicitud contiene campos inválidos\",\"path\":\"/api/v1/auth/login\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":{\"email\":\"El email debe ser válido\"}}"))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Credenciales inválidas\",\"path\":\"/api/v1/auth/login\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/auth/login\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}")))
    })
    public ResponseEntity<AuthResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.iniciarSesion(request));
    }
}
