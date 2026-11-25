package com.universidad.climaplan.exception;

public class TwoFactorVerificationFailedException extends RuntimeException {

    public TwoFactorVerificationFailedException() {
        super("El código de acceso es inválido o expiró.");
    }
}
