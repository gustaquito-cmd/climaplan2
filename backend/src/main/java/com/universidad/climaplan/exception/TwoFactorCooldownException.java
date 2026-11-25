package com.universidad.climaplan.exception;

public class TwoFactorCooldownException extends RuntimeException {

    public TwoFactorCooldownException() {
        super("Espera 60 segundos antes de solicitar otro código de acceso.");
    }
}
