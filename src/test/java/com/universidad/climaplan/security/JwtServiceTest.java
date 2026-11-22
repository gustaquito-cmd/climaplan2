package com.universidad.climaplan.security;

import com.universidad.climaplan.entity.Rol;
import com.universidad.climaplan.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void generaYValidaTokenConClaimsEsperados() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        Usuario usuario = usuario();

        String token = jwtService.generarAccessToken(usuario);
        var claims = jwtService.extraerClaims(token);

        assertEquals("ana@example.com", claims.getSubject());
        assertEquals("ROLE_USER", claims.get("rol"));
        assertEquals(42L, ((Number) claims.get("userId")).longValue());
        assertTrue(claims.getIssuedAt() != null);
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
        assertTrue(jwtService.esTokenValido(
                token,
                User.withUsername("ana@example.com")
                        .password("unused")
                        .authorities("ROLE_USER")
                        .build()
        ));
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtService emisor = new JwtService(SECRET, 60_000);
        byte[] otherKey = new byte[32];
        Arrays.fill(otherKey, (byte) 1);
        JwtService validador = new JwtService(
                Base64.getEncoder().encodeToString(otherKey),
                60_000
        );
        Usuario usuario = usuario();
        String token = emisor.generarAccessToken(usuario);

        assertFalse(validador.esTokenValido(token, userDetails()));
    }

    @Test
    void rechazaClaveInsuficiente() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

        assertThrows(IllegalStateException.class, () -> new JwtService(shortSecret, 60_000));
    }

    @Test
    void rechazaTokenExpirado() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1);
        String token = jwtService.generarAccessToken(usuario());
        Thread.sleep(10);

        assertFalse(jwtService.esTokenValido(token, userDetails()));
    }

    private UserDetails userDetails() {
        return User.withUsername("ana@example.com")
                .password("unused")
                .authorities("ROLE_USER")
                .build();
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 42L);
        usuario.setEmail("ana@example.com");
        usuario.setRol(Rol.ROLE_USER);
        return usuario;
    }
}
