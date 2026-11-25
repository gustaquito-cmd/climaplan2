package com.universidad.climaplan.service;

import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ConfigurableEmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> senderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void logsSafeAuthenticationDiagnosticsWithoutSecrets(CapturedOutput output) {
        when(senderProvider.getIfAvailable()).thenReturn(mailSender);
        MailAuthenticationException failure = new MailAuthenticationException(
                new AuthenticationFailedException("535 5.7.8 user@example.com secret-password 123456"));
        doThrow(failure).when(mailSender).send(any(SimpleMailMessage.class));

        EmailDeliveryUnavailableException thrown = assertThrows(EmailDeliveryUnavailableException.class,
                () -> service("smtp", "smtp.example.test").sendTwoFactorCode("user@example.com", "123456"));

        assertSame(failure, thrown.getCause());
        assertTrue(output.getOut().contains("provider=smtp, host=smtp.example.test, port=587"));
        assertTrue(output.getOut().contains("MailAuthenticationException"));
        assertTrue(output.getOut().contains("AuthenticationFailedException"));
        assertTrue(output.getOut().contains("autenticación SMTP rechazada"));
        assertTrue(output.getOut().contains("códigoSMTP=535"));
        assertNoSecrets(output);
    }

    @Test
    void doesNotLogFailedMessageContents(CapturedOutput output) {
        when(senderProvider.getIfAvailable()).thenReturn(mailSender);
        SimpleMailMessage failedMessage = new SimpleMailMessage();
        failedMessage.setTo("user@example.com");
        failedMessage.setText("123456 secret-password");
        MailSendException failure = new MailSendException(Map.of(
                failedMessage, new MessagingException("550 user@example.com secret-password 123456")));
        doThrow(failure).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(EmailDeliveryUnavailableException.class,
                () -> service("smtp", "smtp.example.test").sendVerificationCode("user@example.com", "123456"));

        assertTrue(output.getOut().contains("MessagingException"));
        assertTrue(output.getOut().contains("códigoSMTP=550"));
        assertNoSecrets(output);
    }

    @Test
    void logsMissingConfigurationWithoutExposingInvalidHost(CapturedOutput output) {
        when(senderProvider.getIfAvailable()).thenReturn(null);

        assertFalse(service("smtp", "user@example.com:secret-password").isConfigured());

        assertTrue(output.getOut().contains("host=no disponible"));
        assertTrue(output.getOut().contains("senderDisponible=false"));
        assertNoSecrets(output);
    }

    private ConfigurableEmailService service(String provider, String host) {
        return new ConfigurableEmailService(senderProvider, provider, "sender@example.com", host, 587, "");
    }

    private void assertNoSecrets(CapturedOutput output) {
        assertFalse(output.getOut().contains("123456"));
        assertFalse(output.getOut().contains("secret-password"));
        assertFalse(output.getOut().contains("user@example.com"));
    }
}
