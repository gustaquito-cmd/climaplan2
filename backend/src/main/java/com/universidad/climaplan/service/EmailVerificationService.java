package com.universidad.climaplan.service;

import com.universidad.climaplan.entity.EmailVerificationCode;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import com.universidad.climaplan.exception.EmailVerificationFailedException;
import com.universidad.climaplan.repository.EmailVerificationCodeRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.security.OtpCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private static final String HASH_CONTEXT = "climaplan/email-verification/otp:v1";

    private final UsuarioRepository usuarioRepository;
    private final EmailVerificationCodeRepository codeRepository;
    private final EmailService emailService;
    private final OtpCodeService otpCodeService;

    public EmailVerificationService(
            UsuarioRepository usuarioRepository,
            EmailVerificationCodeRepository codeRepository,
            EmailService emailService,
            OtpCodeService otpCodeService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.codeRepository = codeRepository;
        this.emailService = emailService;
        this.otpCodeService = otpCodeService;
    }

    @Transactional
    public void sendVerificationCode(Usuario usuario) {
        ensureEmailConfigured();
        invalidateUnusedCodes(usuario);
        issueCode(usuario);
    }

    @Transactional(noRollbackFor = EmailVerificationFailedException.class)
    public void verify(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        Usuario usuario = usuarioRepository.findByEmailForUpdate(normalizedEmail)
                .orElseThrow(EmailVerificationFailedException::new);
        if (usuario.isEmailVerified()) {
            throw new EmailVerificationFailedException();
        }

        EmailVerificationCode verificationCode = codeRepository
                .findFirstByUsuario_IdAndUsedFalseOrderByCreatedAtDescIdDesc(usuario.getId())
                .orElseThrow(EmailVerificationFailedException::new);
        Instant now = Instant.now();
        if (!verificationCode.getExpiresAt().isAfter(now)) {
            verificationCode.setUsed(true);
            codeRepository.save(verificationCode);
            throw new EmailVerificationFailedException();
        }
        if (verificationCode.getAttempts() >= MAX_ATTEMPTS) {
            verificationCode.setUsed(true);
            codeRepository.save(verificationCode);
            throw new EmailVerificationFailedException();
        }

        if (otpCodeService.matches(HASH_CONTEXT, normalizedEmail, code, verificationCode.getCodeHash())) {
            verificationCode.setUsed(true);
            usuario.setEmailVerified(true);
            codeRepository.save(verificationCode);
            usuarioRepository.save(usuario);
            return;
        }

        verificationCode.setAttempts(verificationCode.getAttempts() + 1);
        if (verificationCode.getAttempts() >= MAX_ATTEMPTS) {
            verificationCode.setUsed(true);
        }
        codeRepository.save(verificationCode);
        throw new EmailVerificationFailedException();
    }

    @Transactional
    public void resend(String email) {
        ensureEmailConfigured();
        String normalizedEmail = normalizeEmail(email);
        usuarioRepository.findByEmailForUpdate(normalizedEmail).ifPresent(usuario -> {
            if (usuario.isEmailVerified()) {
                return;
            }

            codeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(usuario.getId())
                    .filter(latest -> latest.getCreatedAt().isAfter(Instant.now().minus(RESEND_COOLDOWN)))
                    .ifPresentOrElse(
                            ignored -> { },
                            () -> {
                                invalidateUnusedCodes(usuario);
                                issueCode(usuario);
                            }
                    );
        });
    }

    private void issueCode(Usuario usuario) {
        String code = otpCodeService.generateCode();
        Instant createdAt = Instant.now();
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUsuario(usuario);
        verificationCode.setCodeHash(
                otpCodeService.hash(HASH_CONTEXT, normalizeEmail(usuario.getEmail()), code)
        );
        verificationCode.setExpiresAt(createdAt.plus(CODE_TTL));
        verificationCode.setCreatedAt(createdAt);
        verificationCode.setAttempts(0);
        verificationCode.setUsed(false);
        codeRepository.save(verificationCode);
        emailService.sendVerificationCode(usuario.getEmail(), code);
    }

    private void invalidateUnusedCodes(Usuario usuario) {
        List<EmailVerificationCode> unusedCodes =
                codeRepository.findAllByUsuario_IdAndUsedFalse(usuario.getId());
        unusedCodes.forEach(code -> code.setUsed(true));
        codeRepository.saveAll(unusedCodes);
    }

    private void ensureEmailConfigured() {
        if (!emailService.isConfigured()) {
            throw new EmailDeliveryUnavailableException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
