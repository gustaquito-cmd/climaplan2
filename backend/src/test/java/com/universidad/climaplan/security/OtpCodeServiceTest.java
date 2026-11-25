package com.universidad.climaplan.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpCodeServiceTest {

    @Test
    void otpHashContextsAreCryptographicallySeparated() {
        OtpCodeService service = new OtpCodeService(
                Base64.getEncoder().encodeToString(new byte[32])
        );
        String emailVerificationHash = service.hash(
                "climaplan/email-verification/otp:v1",
                "user@example.com",
                "123456"
        );
        String twoFactorHash = service.hash(
                "climaplan/auth/2fa/otp:v1",
                "user@example.com",
                "123456"
        );

        assertNotEquals(emailVerificationHash, twoFactorHash);
        assertTrue(service.matches(
                "climaplan/auth/2fa/otp:v1",
                "user@example.com",
                "123456",
                twoFactorHash
        ));
    }
}
