package com.universidad.climaplan.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "CORS_ALLOWED_ORIGINS=http://localhost:5173, https://eloquent-encouragement-production-fd1af15a.up.railway.app",
                "spring.datasource.url=jdbc:h2:mem:cors-test;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
        }
)
class CorsIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:5173",
            "https://eloquent-encouragement-production-fd1af15a.up.railway.app"
    })
    void configuredOriginsAreAllowedForRestAndWebSocket(String origin) throws Exception {
        HttpResponse<Void> response = preflight(origin);
        assertEquals(200, response.statusCode());
        assertEquals(origin, response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertTrue(response.headers().firstValue("Access-Control-Allow-Credentials").isEmpty());

        WebSocket socket = connect(origin).get(10, TimeUnit.SECONDS);
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
    }

    @Test
    void untrustedOriginIsRejectedForRestAndWebSocket() throws Exception {
        String origin = "https://untrusted.example.com";
        HttpResponse<Void> response = preflight(origin);
        assertEquals(403, response.statusCode());
        assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> connect(origin).get(10, TimeUnit.SECONDS));
        WebSocketHandshakeException handshake = assertInstanceOf(
                WebSocketHandshakeException.class, failure.getCause());
        assertEquals(403, handshake.getResponse().statusCode());
    }

    private HttpResponse<Void> preflight(String origin) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private java.util.concurrent.CompletableFuture<WebSocket> connect(String origin) {
        return client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .header("Origin", origin)
                .buildAsync(URI.create("ws://localhost:" + port + "/ws"), new WebSocket.Listener() { });
    }
}
