package com.universidad.climaplan.service;

public interface EmailService {

    boolean isConfigured();

    void sendVerificationCode(String email, String code);

    void sendTwoFactorCode(String email, String code);
}
