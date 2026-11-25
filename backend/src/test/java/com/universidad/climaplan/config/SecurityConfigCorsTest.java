package com.universidad.climaplan.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigCorsTest {

    @Test
    void allowsOnlyConfiguredFrontendOriginsWithoutCredentials() {
        SecurityConfig security = new SecurityConfig();
        var source = security.corsConfigurationSource(security.corsConfiguration(
                List.of("http://localhost:5173", " https://eloquent-encouragement-production-fd1af15a.up.railway.app ")
        ));
        var request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/register");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals(
                List.of("http://localhost:5173", "https://eloquent-encouragement-production-fd1af15a.up.railway.app"),
                configuration.getAllowedOrigins()
        );
        assertTrue(configuration.getAllowedMethods().contains("POST"));
        assertTrue(configuration.getAllowedHeaders().contains("Authorization"));
        assertFalse(configuration.getAllowCredentials());
        assertNotNull(configuration.checkOrigin("http://localhost:5173"));
        assertNotNull(configuration.checkOrigin("https://eloquent-encouragement-production-fd1af15a.up.railway.app"));
        assertNull(configuration.checkOrigin("https://untrusted.example.com"));
        assertEquals(configuration, source.getCorsConfiguration(new MockHttpServletRequest("GET", "/ws")));
    }

    @Test
    void rejectsWildcardOrEmptyOrigins() {
        SecurityConfig security = new SecurityConfig();
        assertThrows(IllegalArgumentException.class, () -> security.corsConfiguration(List.of("*")));
        assertThrows(IllegalArgumentException.class, () -> security.corsConfiguration(List.of(" ")));
        assertThrows(IllegalArgumentException.class, () -> security.corsConfiguration(List.of()));
    }
}
