package com.universidad.climaplan.service;

import com.universidad.climaplan.entity.EmailVerificationCode;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.EmailVerificationFailedException;
import com.universidad.climaplan.repository.EmailVerificationCodeRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.security.OtpCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailVerificationCodeRepository codeRepository;

    @Mock
    private EmailService emailService;

    private EmailVerificationService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                usuarioRepository,
                codeRepository,
                emailService,
                new OtpCodeService(Base64.getEncoder().encodeToString(new byte[32]))
        );
        usuario = new Usuario();
        usuario.setEmail("user@example.com");
        ReflectionTestUtils.setField(usuario, "id", 1L);
        when(emailService.isConfigured()).thenReturn(true);
    }

    @Test
    void issuesSixDigitOtpAndPersistsOnlyItsHash() {
        Instant beforeIssue = Instant.now();
        service.sendVerificationCode(usuario);

        ArgumentCaptor<EmailVerificationCode> codeCaptor =
                ArgumentCaptor.forClass(EmailVerificationCode.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(codeRepository).save(codeCaptor.capture());
        verify(emailService).sendVerificationCode(eq(usuario.getEmail()), otpCaptor.capture());

        String otp = otpCaptor.getValue();
        EmailVerificationCode persisted = codeCaptor.getValue();
        assertTrue(Pattern.matches("[0-9]{6}", otp));
        assertNotEquals(otp, persisted.getCodeHash());
        assertTrue(Pattern.matches("[0-9a-f]{64}", persisted.getCodeHash()));
        assertTrue(persisted.getExpiresAt().isAfter(beforeIssue.plus(Duration.ofMinutes(5)).minusSeconds(1)));
        assertTrue(persisted.getExpiresAt().isBefore(beforeIssue.plus(Duration.ofMinutes(5)).plusSeconds(1)));
        assertEquals(0, persisted.getAttempts());
        assertFalse(persisted.isUsed());
    }

    @Test
    void verifiesCorrectOtpAndCannotReuseIt() {
        String otp = issueOtp();
        EmailVerificationCode savedCode = savedCode();
        when(usuarioRepository.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(codeRepository.findFirstByUsuario_IdAndUsedFalseOrderByCreatedAtDescIdDesc(1L))
                .thenAnswer(invocation -> savedCode.isUsed() ? Optional.empty() : Optional.of(savedCode));

        service.verify(usuario.getEmail(), otp);

        assertTrue(usuario.isEmailVerified());
        assertTrue(savedCode.isUsed());
        verify(usuarioRepository).save(usuario);
        assertThrows(EmailVerificationFailedException.class, () -> service.verify(usuario.getEmail(), otp));
    }

    @Test
    void incrementsAttemptsForIncorrectOtp() {
        String otp = issueOtp();
        EmailVerificationCode savedCode = savedCode();
        when(usuarioRepository.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(codeRepository.findFirstByUsuario_IdAndUsedFalseOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(Optional.of(savedCode));

        assertThrows(EmailVerificationFailedException.class, () ->
                service.verify(usuario.getEmail(), otp.equals("000000") ? "000001" : "000000"));

        assertEquals(1, savedCode.getAttempts());
        assertFalse(savedCode.isUsed());
    }

    @Test
    void expiresOtpAndMarksItUsed() {
        issueOtp();
        EmailVerificationCode savedCode = savedCode();
        savedCode.setExpiresAt(Instant.now().minusSeconds(1));
        clearInvocations(codeRepository);
        when(usuarioRepository.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(codeRepository.findFirstByUsuario_IdAndUsedFalseOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(Optional.of(savedCode));

        assertThrows(EmailVerificationFailedException.class, () ->
                service.verify(usuario.getEmail(), "123456"));

        assertTrue(savedCode.isUsed());
        verify(codeRepository).save(savedCode);
    }

    @Test
    void disablesOtpAfterFiveIncorrectAttempts() {
        String otp = issueOtp();
        String wrongCode = otp.equals("000000") ? "000001" : "000000";
        EmailVerificationCode savedCode = savedCode();
        when(usuarioRepository.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(codeRepository.findFirstByUsuario_IdAndUsedFalseOrderByCreatedAtDescIdDesc(1L))
                .thenAnswer(invocation -> savedCode.isUsed() ? Optional.empty() : Optional.of(savedCode));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(EmailVerificationFailedException.class, () ->
                    service.verify(usuario.getEmail(), wrongCode));
        }

        assertEquals(5, savedCode.getAttempts());
        assertTrue(savedCode.isUsed());
        assertThrows(EmailVerificationFailedException.class, () ->
                service.verify(usuario.getEmail(), wrongCode));
    }

    @Test
    void resendInvalidatesPreviousOtpAndIssuesAnother() {
        EmailVerificationCode previous = new EmailVerificationCode();
        previous.setUsuario(usuario);
        previous.setCreatedAt(Instant.now().minusSeconds(61));
        previous.setExpiresAt(Instant.now().plusSeconds(120));
        previous.setCodeHash("previous-hash");
        when(usuarioRepository.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(codeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(Optional.of(previous));
        when(codeRepository.findAllByUsuario_IdAndUsedFalse(1L))
                .thenReturn(new ArrayList<>(List.of(previous)));

        service.resend(usuario.getEmail());

        assertTrue(previous.isUsed());
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(eq(usuario.getEmail()), otpCaptor.capture());
        assertTrue(Pattern.matches("[0-9]{6}", otpCaptor.getValue()));
        verify(codeRepository).saveAll(List.of(previous));
    }

    @Test
    void resendWithinCooldownDoesNotInvalidateOrSendAnotherOtp() {
        EmailVerificationCode previous = new EmailVerificationCode();
        previous.setUsuario(usuario);
        previous.setCreatedAt(Instant.now().minusSeconds(30));
        previous.setExpiresAt(Instant.now().plusSeconds(120));
        previous.setCodeHash("previous-hash");
        when(usuarioRepository.findByEmailForUpdate(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(codeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(Optional.of(previous));

        service.resend(usuario.getEmail());

        assertFalse(previous.isUsed());
        verify(emailService, never()).sendVerificationCode(any(), any());
    }

    @Test
    void resendReturnsGenericSuccessWithoutSendingWhenEmailIsUnknown() {
        when(usuarioRepository.findByEmailForUpdate("unknown@example.com")).thenReturn(Optional.empty());

        service.resend("unknown@example.com");

        verify(emailService, never()).sendVerificationCode(any(), any());
    }

    private String issueOtp() {
        when(codeRepository.findAllByUsuario_IdAndUsedFalse(1L)).thenReturn(new ArrayList<>());
        service.sendVerificationCode(usuario);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(eq(usuario.getEmail()), otpCaptor.capture());
        return otpCaptor.getValue();
    }

    private EmailVerificationCode savedCode() {
        ArgumentCaptor<EmailVerificationCode> codeCaptor =
                ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codeRepository).save(codeCaptor.capture());
        return codeCaptor.getValue();
    }
}
