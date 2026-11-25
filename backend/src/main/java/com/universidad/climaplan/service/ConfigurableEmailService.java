package com.universidad.climaplan.service;

import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ConfigurableEmailService implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String provider;
    private final String from;
    private final String host;

    public ConfigurableEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.email.provider:none}") String provider,
            @Value("${app.email.from:}") String from,
            @Value("${spring.mail.host:}") String host
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.provider = provider;
        this.from = from;
        this.host = host;
    }

    @Override
    public boolean isConfigured() {
        return "smtp".equalsIgnoreCase(provider)
                && !from.isBlank()
                && !host.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
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
            throw new EmailDeliveryUnavailableException(exception);
        }
    }
}
