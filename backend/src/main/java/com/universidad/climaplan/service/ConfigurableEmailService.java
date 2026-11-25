package com.universidad.climaplan.service;

import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConfigurableEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableEmailService.class);
    private static final Pattern SMTP_STATUS = Pattern.compile("^\\s*([245][0-5][0-9])(?:[ -]|$)");
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String provider;
    private final String from;
    private final String host;
    private final int port;

    public ConfigurableEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.email.provider:none}") String provider,
            @Value("${app.email.from:}") String from,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.provider = provider;
        this.from = from;
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean isConfigured() {
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
