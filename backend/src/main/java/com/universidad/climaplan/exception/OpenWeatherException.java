package com.universidad.climaplan.exception;

import org.springframework.http.HttpStatus;

public class OpenWeatherException extends RuntimeException {

    private final HttpStatus status;

    public OpenWeatherException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public OpenWeatherException(HttpStatus status, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
