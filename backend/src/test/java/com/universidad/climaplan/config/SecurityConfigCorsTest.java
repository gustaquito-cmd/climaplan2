package com.universidad.climaplan.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigCorsTest {

    @Test
    void allowsOnlyConfiguredFrontendOriginsWithoutCredentials() {
        var source = new SecurityConfig().corsConfigurationSource(
                List.of("http://localhost:5173", "https://frontend.example.com")
        );
        var request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/register");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals(
                List.of("http://localhost:5173", "https://frontend.example.com"),
                configuration.getAllowedOrigins()
        );
        assertTrue(configuration.getAllowedMethods().contains("POST"));
        assertTrue(configuration.getAllowedHeaders().contains("Authorization"));
        assertFalse(configuration.getAllowCredentials());
        assertTrue(configuration.checkOrigin("https://frontend.example.com") != null);
        assertNull(configuration.checkOrigin("https://untrusted.example.com"));
    }
}
