package com.universidad.climaplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConfigurableEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableEmailService.class);
    private static final Pattern SMTP_STATUS = Pattern.compile("^\\s*([245][0-5][0-9])(?:[ -]|$)");
    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String provider;
    private final String from;
    private final String host;
    private final int port;
    private final String brevoApiKey;
    private final RestClient brevoClient;

    @Autowired
    public ConfigurableEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.email.provider:none}") String provider,
            @Value("${app.email.from:}") String from,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${app.email.brevo-api-key:}") String brevoApiKey
    ) {
        this(mailSenderProvider, provider, from, host, port, brevoApiKey, createBrevoClient());
    }

    ConfigurableEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            String provider, String from, String host, int port, String brevoApiKey, RestClient brevoClient
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.provider = provider;
        this.from = from;
        this.host = host;
        this.port = port;
        this.brevoApiKey = brevoApiKey;
        this.brevoClient = brevoClient;
    }

    @Override
    public boolean isConfigured() {
        if ("brevo_api".equalsIgnoreCase(provider)) {
            boolean configured = !from.isBlank() && !brevoApiKey.isBlank();
            if (!configured) {
                LOGGER.warn("Brevo API no configurada: remitentePresente={}, clavePresente={}",
                        !from.isBlank(), !brevoApiKey.isBlank());
            }
            return configured;
        }
        boolean configured = "smtp".equalsIgnoreCase(provider)
                && !from.isBlank()
                && !host.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
        if (!configured) {
            LOGGER.warn("SMTP no configurado: provider={}, host={}, port={}, remitentePresente={}, senderDisponible={}",
                    safeProvider(), safeHost(), port, !from.isBlank(), mailSenderProvider.getIfAvailable() != null);
        }
        return configured;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        sendCode(email, code, "Verifica tu correo de ClimaPlan", """
                Tu código de verificación de ClimaPlan es: %s

                El código vence en 5 minutos. Si no solicitaste esta verificación, ignora este mensaje.
                """.formatted(code));
    }

    @Override
    public void sendTwoFactorCode(String email, String code) {
        sendCode(email, code, "Tu código de acceso de ClimaPlan", """
                Tu código de acceso de ClimaPlan es: %s

                El código vence en 5 minutos. Si no intentaste iniciar sesión, cambia tu contraseña.
                """.formatted(code));
    }

    private void sendCode(String email, String code, String subject, String body) {
        if ("brevo_api".equalsIgnoreCase(provider)) {
            sendWithBrevo(email, subject, body);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!"smtp".equalsIgnoreCase(provider) || from.isBlank() || host.isBlank() || mailSender == null) {
            LOGGER.warn("SMTP no configurado: provider={}, host={}, port={}, remitentePresente={}, senderDisponible={}",
                    safeProvider(), safeHost(), port, !from.isBlank(), mailSender != null);
            throw new EmailDeliveryUnavailableException();
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logSmtpFailure(exception);
            throw new EmailDeliveryUnavailableException(exception);
        }
    }

    private void sendWithBrevo(String email, String subject, String body) {
        if (!isConfigured()) {
            throw new EmailDeliveryUnavailableException();
        }
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", "ClimaPlan", "email", from),
                "to", List.of(Map.of("email", email)),
                "subject", subject,
                "textContent", body
        );
        try {
            brevoClient.post()
                    .uri(BREVO_ENDPOINT)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", brevoApiKey)
                    // RestClient logs object bodies at DEBUG; byte arrays do not expose their contents.
                    .body(JSON_MAPPER.writeValueAsBytes(payload))
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            LOGGER.warn("Brevo API rechazó el envío: status={}", response.getStatusCode().value());
                            throw new EmailDeliveryUnavailableException();
                        }
                        return null;
                    });
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Brevo API no disponible: no se pudo preparar la solicitud");
            throw new EmailDeliveryUnavailableException();
        } catch (RestClientException exception) {
            LOGGER.warn("Brevo API no disponible: fallo de transporte, tipo={}",
                    exception.getClass().getSimpleName());
            throw new EmailDeliveryUnavailableException();
        }
    }

    private static RestClient createBrevoClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private void logSmtpFailure(MailException exception) {
        Throwable current = exception;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        String smtpStatus = "no disponible";
        while (visited.add(current)) {
            String message = current.getMessage();
            if (message != null) {
                Matcher matcher = SMTP_STATUS.matcher(message);
                if (matcher.find()) {
                    smtpStatus = matcher.group(1);
                }
            }
            Throwable next = current.getCause();
            if (next == null && current instanceof MailSendException sendException
                    && !sendException.getFailedMessages().isEmpty()) {
                next = sendException.getFailedMessages().values().iterator().next();
            }
            if (next == null && current instanceof MessagingException messagingException) {
                next = messagingException.getNextException();
            }
            if (next == null || visited.contains(next)) {
                break;
            }
            current = next;
        }

        String detail = "fallo del transporte SMTP";
        if (exception instanceof MailAuthenticationException || current instanceof AuthenticationFailedException) {
            detail = "autenticación SMTP rechazada";
        } else if (current instanceof UnknownHostException) {
            detail = "host SMTP no resuelto";
        } else if (current instanceof SocketTimeoutException) {
            detail = "tiempo de conexión o lectura agotado";
        } else if (current instanceof ConnectException) {
            detail = "conexión SMTP rechazada o inaccesible";
        } else if (current instanceof SSLException) {
            detail = "fallo de negociación TLS";
        }
        LOGGER.warn("SMTP falló: provider={}, host={}, port={}, excepción={}, causa={}, detalle={}, códigoSMTP={}",
                safeProvider(), safeHost(), port, exception.getClass().getName(),
                current.getClass().getName(), detail, smtpStatus);
    }

    private String safeProvider() {
        if ("smtp".equalsIgnoreCase(provider)) {
            return "smtp";
        }
        return "none".equalsIgnoreCase(provider) || provider.isBlank() ? "none" : "no reconocido";
    }

    private String safeHost() {
        return host.matches("[A-Za-z0-9][A-Za-z0-9.-]{0,252}") ? host : "no disponible";
    }
}
