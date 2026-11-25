package com.universidad.climaplan.exception;

import com.universidad.climaplan.dto.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarErroresValidacion(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errores = new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                400, "Bad Request", "La solicitud contiene campos inválidos",
                request.getRequestURI(), LocalDateTime.now(), errores
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> manejarViolacionesDeParametros(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                400,
                "Bad Request",
                "La solicitud contiene parámetros inválidos",
                request.getRequestURI(),
                LocalDateTime.now(),
                null
        ));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        404, "Not Found", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ApiErrorResponse> manejarEmailDuplicado(
            EmailDuplicadoException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        409, "Conflict", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiErrorResponse> manejarEmailNoVerificado(
            EmailNotVerifiedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(
                        403, "Forbidden", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(EmailVerificationFailedException.class)
    public ResponseEntity<ApiErrorResponse> manejarVerificacionFallida(
            EmailVerificationFailedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        400, "Bad Request", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(EmailDeliveryUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> manejarCorreoNoDisponible(
            EmailDeliveryUnavailableException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        503, "Service Unavailable", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(TwoFactorVerificationFailedException.class)
    public ResponseEntity<ApiErrorResponse> manejarVerificacionDosFactoresFallida(
            TwoFactorVerificationFailedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        400, "Bad Request", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(TwoFactorCooldownException.class)
    public ResponseEntity<ApiErrorResponse> manejarCooldownDosFactores(
            TwoFactorCooldownException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse(
                        429, "Too Many Requests", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(ConflictoRecursoException.class)
    public ResponseEntity<ApiErrorResponse> manejarConflictoRecurso(
            ConflictoRecursoException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        409, "Conflict", exception.getMessage(),
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> manejarCredencialesInvalidas(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        401, "Unauthorized", "Credenciales inválidas",
                        request.getRequestURI(), LocalDateTime.now(), null
                ));
    }

    @ExceptionHandler(OpenWeatherException.class)
    public ResponseEntity<ApiErrorResponse> manejarErrorOpenWeather(
            OpenWeatherException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(
                        exception.getStatus().value(),
                        exception.getStatus().getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarErrorInterno(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        500,
                        "Internal Server Error",
                        "Ocurrió un error interno controlado",
                        request.getRequestURI(),
                        LocalDateTime.now(),
                        null
                ));
    }
}
