package com.universidad.climaplan.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class OtpCodeService {

    private final byte[] masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpCodeService(@Value("${app.jwt.secret}") String jwtSecret) {
        this.masterKey = Base64.getDecoder().decode(jwtSecret);
        if (masterKey.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe decodificar al menos 32 bytes");
        }
    }

    public String generateCode() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    public String hash(String context, String subject, String code) {
        byte[] contextKey = hmac(masterKey, context);
        return HexFormat.of().formatHex(hmac(contextKey, subject + ":" + code));
    }

    public boolean matches(String context, String subject, String code, String expectedHash) {
        byte[] candidate = hash(context, subject, code).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(candidate, expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] hmac(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo calcular el hash del código", exception);
        }
    }
}
