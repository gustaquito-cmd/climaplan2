package com.universidad.climaplan.exception;

public class EmailDeliveryUnavailableException extends RuntimeException {

    public EmailDeliveryUnavailableException() {
        super("El envío de correo no está configurado o no está disponible.");
    }

    public EmailDeliveryUnavailableException(Throwable cause) {
        super("El envío de correo no está configurado o no está disponible.", cause);
    }
}
