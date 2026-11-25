package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.auth.AuthOperationResponse;
import com.universidad.climaplan.dto.auth.AuthLoginResponse;
import com.universidad.climaplan.dto.auth.AuthResponse;
import com.universidad.climaplan.dto.auth.LoginRequest;
import com.universidad.climaplan.dto.auth.RegisterRequest;
import com.universidad.climaplan.dto.auth.ResendVerificationRequest;
import com.universidad.climaplan.dto.auth.VerifyEmailRequest;
import com.universidad.climaplan.dto.auth.VerifyTwoFactorRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Registro, verificación de correo e inicio de sesión")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea una cuenta no verificada y envía un código de verificación.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitud de registro aceptada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthOperationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"La solicitud contiene campos inválidos\",\"path\":\"/api/v1/auth/register\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":{\"email\":\"El email debe ser válido\"}}"))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/auth/register\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "503", description = "Envío de correo no configurado o no disponible",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthOperationResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.accepted().body(authService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida credenciales y, si 2FA está habilitado, solicita un OTP antes de emitir JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthLoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Bad Request", value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"La solicitud contiene campos inválidos\",\"path\":\"/api/v1/auth/login\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":{\"email\":\"El email debe ser válido\"}}"))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Credenciales inválidas\",\"path\":\"/api/v1/auth/login\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "403", description = "Correo no verificado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(name = "Internal Server Error", value = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Ocurrió un error interno en el servidor\",\"path\":\"/api/v1/auth/login\",\"timestamp\":\"2026-11-20T12:00:00\",\"validationErrors\":null}"))),
            @ApiResponse(responseCode = "503", description = "Envío del código 2FA no disponible",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthLoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.iniciarSesion(request));
    }

    @PostMapping("/verify-2fa")
    @Operation(summary = "Verificar código 2FA", description = "Valida un challenge OTP y devuelve el JWT normal al completarse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Segundo factor verificado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Challenge o código inválido o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> verificarDosFactores(
            @Valid @RequestBody VerifyTwoFactorRequest request
    ) {
        return ResponseEntity.ok(authService.verificarDosFactores(request));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verificar correo", description = "Valida el código OTP de seis dígitos enviado al correo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correo verificado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthOperationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado o datos incorrectos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthOperationResponse> verificarCorreo(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        return ResponseEntity.ok(authService.verificarCorreo(request));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar código", description = "Solicita el reenvío del código sin revelar si el correo está registrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitud aceptada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthOperationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Correo inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Envío de correo no configurado o no disponible",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AuthOperationResponse> reenviarVerificacion(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        return ResponseEntity.accepted().body(authService.reenviarVerificacion(request));
    }
}
