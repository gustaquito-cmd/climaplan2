package com.universidad.climaplan.exception;

public class EmailVerificationFailedException extends RuntimeException {

    public EmailVerificationFailedException() {
        super("El código es inválido o expiró.");
    }
}
