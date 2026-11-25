package com.universidad.climaplan.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BrevoEmailServiceTest {

    private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private static final String API_KEY = "test-api-key";
    private static final String OTP = "123456";

    @Mock
    private ObjectProvider<JavaMailSender> senderProvider;

    private RestClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
    }

    @Test
    void sendsVerificationEmailUsingBrevoApi() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(header("accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.name").value("ClimaPlan"))
                .andExpect(jsonPath("$.sender.email").value("sender@example.com"))
                .andExpect(jsonPath("$.to.length()").value(1))
                .andExpect(jsonPath("$.to[0].email").value("recipient@example.com"))
                .andExpect(jsonPath("$.subject").value("Verifica tu correo de ClimaPlan"))
                .andExpect(jsonPath("$.textContent").value(containsString(OTP)))
                .andRespond(withStatus(HttpStatusCode.valueOf(201)));

        ConfigurableEmailService service = service(API_KEY);
        assertTrue(service.isConfigured());
        service.sendVerificationCode("recipient@example.com", OTP);

        server.verify();
        verifyNoInteractions(senderProvider);
    }

    @Test
    void sendsTwoFactorEmailAndAcceptsOtherSuccessfulStatus() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(jsonPath("$.subject").value("Tu código de acceso de ClimaPlan"))
                .andExpect(jsonPath("$.textContent").value(containsString(OTP)))
                .andRespond(withStatus(HttpStatusCode.valueOf(204)));

        service(API_KEY).sendTwoFactorCode("recipient@example.com", OTP);

        server.verify();
    }

    @Test
    void debugLoggingDoesNotExposeRequestBodyOrKey(CapturedOutput output) {
        Logger logger = (Logger) LoggerFactory.getLogger("org.springframework.web.client.DefaultRestClient");
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        try {
            server.expect(requestTo(ENDPOINT))
                    .andRespond(withStatus(HttpStatusCode.valueOf(201)));

            service(API_KEY).sendVerificationCode("recipient@example.com", OTP);

            server.verify();
            assertTrue(output.getOut().contains("Writing ["));
            assertSafeOutput(output);
        } finally {
            logger.setLevel(previousLevel);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 400, 401, 403, 429, 500, 503})
    void rejectsNonSuccessResponsesWithoutLoggingSensitiveData(int status, CapturedOutput output) {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatusCode.valueOf(status))
                        .body("recipient@example.com " + API_KEY + " " + OTP));

        assertThrows(EmailDeliveryUnavailableException.class,
                () -> service(API_KEY).sendVerificationCode("recipient@example.com", OTP));

        server.verify();
        assertTrue(output.getOut().contains("status=" + status));
        assertSafeOutput(output);
    }

    @Test
    void convertsConnectionFailureWithoutLoggingExceptionMessage(CapturedOutput output) {
        server.expect(requestTo(ENDPOINT))
                .andRespond(request -> {
                    throw new IOException("recipient@example.com " + API_KEY + " " + OTP);
                });

        assertThrows(EmailDeliveryUnavailableException.class,
                () -> service(API_KEY).sendTwoFactorCode("recipient@example.com", OTP));

        server.verify();
        assertTrue(output.getOut().contains("fallo de transporte"));
        assertSafeOutput(output);
    }

    @Test
    void missingApiKeyIsNotConfiguredAndDoesNotSend() {
        ConfigurableEmailService service = service(" ");
        assertFalse(service.isConfigured());
        assertThrows(EmailDeliveryUnavailableException.class,
                () -> service.sendVerificationCode("recipient@example.com", OTP));

        server.verify();
        verifyNoInteractions(senderProvider);
    }

    private ConfigurableEmailService service(String apiKey) {
        return new ConfigurableEmailService(
                senderProvider, "brevo_api", "sender@example.com", "", 587, apiKey, client
        );
    }

    private void assertSafeOutput(CapturedOutput output) {
        assertFalse(output.toString().contains(API_KEY));
        assertFalse(output.toString().contains(OTP));
        assertFalse(output.toString().contains("recipient@example.com"));
    }
}
