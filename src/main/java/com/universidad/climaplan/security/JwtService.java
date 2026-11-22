package com.universidad.climaplan.security;

import com.universidad.climaplan.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(base64Secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("app.jwt.secret debe ser una clave Base64 válida", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe decodificar al menos 32 bytes para HS256");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("app.jwt.expiration-ms debe ser mayor que cero");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generarAccessToken(Usuario usuario) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("userId", usuario.getId())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esTokenValido(String token, UserDetails userDetails) {
        try {
            Claims claims = extraerClaims(token);
            return claims.getSubject() != null
                    && claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long obtenerExpiracionMs() {
        return expirationMs;
    }
}
