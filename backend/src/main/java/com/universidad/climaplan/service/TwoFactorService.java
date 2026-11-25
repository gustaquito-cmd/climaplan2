package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.auth.TwoFactorRequiredResponse;
import com.universidad.climaplan.entity.TwoFactorChallenge;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.EmailDeliveryUnavailableException;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.exception.TwoFactorCooldownException;
import com.universidad.climaplan.exception.TwoFactorVerificationFailedException;
import com.universidad.climaplan.repository.TwoFactorChallengeRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.security.OtpCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TwoFactorService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final Duration CHALLENGE_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private static final String HASH_CONTEXT = "climaplan/auth/2fa/otp:v1";

    private final UsuarioRepository usuarioRepository;
    private final TwoFactorChallengeRepository challengeRepository;
    private final EmailService emailService;
    private final OtpCodeService otpCodeService;

    public TwoFactorService(
            UsuarioRepository usuarioRepository,
            TwoFactorChallengeRepository challengeRepository,
            EmailService emailService,
            OtpCodeService otpCodeService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.challengeRepository = challengeRepository;
        this.emailService = emailService;
        this.otpCodeService = otpCodeService;
    }

    @Transactional
    public TwoFactorRequiredResponse createChallenge(Usuario usuario) {
        if (!emailService.isConfigured()) {
            throw new EmailDeliveryUnavailableException();
        }

        Usuario lockedUser = usuarioRepository.findByIdForUpdate(usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario"));
        Instant now = Instant.now();
        TwoFactorChallenge latestChallenge = challengeRepository
                .findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(lockedUser.getId())
                .orElse(null);
        if (latestChallenge != null
                && latestChallenge.getCreatedAt().plus(CHALLENGE_COOLDOWN).isAfter(now)) {
            throw new TwoFactorCooldownException();
        }

        List<TwoFactorChallenge> activeChallenges =
                challengeRepository.findAllByUsuario_IdAndUsedFalse(lockedUser.getId());
        activeChallenges.forEach(challenge -> challenge.setUsed(true));
        challengeRepository.saveAll(activeChallenges);

        UUID challengeId = UUID.randomUUID();
        String code = otpCodeService.generateCode();
        Instant createdAt = now;
        TwoFactorChallenge challenge = new TwoFactorChallenge();
        challenge.setId(challengeId);
        challenge.setUsuario(lockedUser);
        challenge.setCodeHash(otpCodeService.hash(
                HASH_CONTEXT,
                challengeSubject(lockedUser, challengeId),
                code
        ));
        challenge.setCreatedAt(createdAt);
        challenge.setExpiresAt(createdAt.plus(CHALLENGE_TTL));
        challenge.setAttempts(0);
        challenge.setUsed(false);
        challengeRepository.save(challenge);
        emailService.sendTwoFactorCode(lockedUser.getEmail(), code);

        return new TwoFactorRequiredResponse(
                true,
                challengeId.toString(),
                "Código enviado a tu correo."
        );
    }

    @Transactional(noRollbackFor = TwoFactorVerificationFailedException.class)
    public Usuario verify(UUID challengeId, String code) {
        TwoFactorChallenge challenge = challengeRepository.findByIdAndUsedFalse(challengeId)
                .orElseThrow(TwoFactorVerificationFailedException::new);
        if (!challenge.getExpiresAt().isAfter(Instant.now())
                || challenge.getAttempts() >= MAX_ATTEMPTS) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            throw new TwoFactorVerificationFailedException();
        }

        Usuario usuario = challenge.getUsuario();
        if (otpCodeService.matches(
                HASH_CONTEXT,
                challengeSubject(usuario, challengeId),
                code,
                challenge.getCodeHash()
        )) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            return usuario;
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        if (challenge.getAttempts() >= MAX_ATTEMPTS) {
            challenge.setUsed(true);
        }
        challengeRepository.save(challenge);
        throw new TwoFactorVerificationFailedException();
    }

    private String challengeSubject(Usuario usuario, UUID challengeId) {
        return usuario.getId() + ":" + usuario.getEmail().trim().toLowerCase(Locale.ROOT)
                + ":" + challengeId;
    }
}
