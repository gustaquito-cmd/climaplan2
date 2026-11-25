package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.auth.TwoFactorRequiredResponse;
import com.universidad.climaplan.entity.TwoFactorChallenge;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.TwoFactorCooldownException;
import com.universidad.climaplan.exception.TwoFactorVerificationFailedException;
import com.universidad.climaplan.repository.TwoFactorChallengeRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.security.OtpCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TwoFactorChallengeRepository challengeRepository;

    @Mock
    private EmailService emailService;

    private final List<TwoFactorChallenge> savedChallenges = new ArrayList<>();
    private final List<String> sentCodes = new ArrayList<>();
    private TwoFactorService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new TwoFactorService(
                usuarioRepository,
                challengeRepository,
                emailService,
                new OtpCodeService(Base64.getEncoder().encodeToString(new byte[32]))
        );
        usuario = user(11L, "owner@example.com");
        when(emailService.isConfigured()).thenReturn(true);
        when(challengeRepository.save(any(TwoFactorChallenge.class))).thenAnswer(invocation -> {
            TwoFactorChallenge challenge = invocation.getArgument(0);
            savedChallenges.add(challenge);
            return challenge;
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            sentCodes.add(invocation.getArgument(1));
            return null;
        }).when(emailService).sendTwoFactorCode(anyString(), anyString());
    }

    @Test
    void createsRandomChallengeAndSendsOnlyEmailOtp() {
        stubIssue(usuario, List.of());

        TwoFactorRequiredResponse response = service.createChallenge(usuario);
        TwoFactorChallenge challenge = latestSavedChallenge();

        assertTrue(response.twoFactorRequired());
        assertEquals(challenge.getId().toString(), response.challengeId());
        assertEquals(usuario, challenge.getUsuario());
        assertTrue(Pattern.matches("[0-9]{6}", sentCodes.get(0)));
        assertNotEquals(sentCodes.get(0), challenge.getCodeHash());
        assertTrue(Pattern.matches("[0-9a-f]{64}", challenge.getCodeHash()));
        assertTrue(challenge.getExpiresAt().isAfter(Instant.now().plusSeconds(299)));
        assertEquals(0, challenge.getAttempts());
        assertFalse(challenge.isUsed());
        assertFalse(response.message().contains(sentCodes.get(0)));
    }

    @Test
    void correctOtpReturnsChallengeUserAndConsumesChallenge() {
        IssuedChallenge issued = issueFor(usuario, List.of());
        when(challengeRepository.findByIdAndUsedFalse(issued.challenge().getId()))
                .thenAnswer(invocation -> issued.challenge().isUsed()
                        ? Optional.empty()
                        : Optional.of(issued.challenge()));

        Usuario verified = service.verify(issued.challenge().getId(), issued.code());

        assertSame(usuario, verified);
        assertTrue(issued.challenge().isUsed());
        assertThrows(TwoFactorVerificationFailedException.class, () ->
                service.verify(issued.challenge().getId(), issued.code()));
    }

    @Test
    void wrongOtpIncrementsAttemptsAndFails() {
        IssuedChallenge issued = issueFor(usuario, List.of());
        when(challengeRepository.findByIdAndUsedFalse(issued.challenge().getId()))
                .thenReturn(Optional.of(issued.challenge()));
        String wrongCode = issued.code().equals("000000") ? "000001" : "000000";

        assertThrows(TwoFactorVerificationFailedException.class, () ->
                service.verify(issued.challenge().getId(), wrongCode));

        assertEquals(1, issued.challenge().getAttempts());
        assertFalse(issued.challenge().isUsed());
    }

    @Test
    void expiredOtpIsConsumedAndRejected() {
        IssuedChallenge issued = issueFor(usuario, List.of());
        issued.challenge().setExpiresAt(Instant.now().minusSeconds(1));
        when(challengeRepository.findByIdAndUsedFalse(issued.challenge().getId()))
                .thenReturn(Optional.of(issued.challenge()));

        assertThrows(TwoFactorVerificationFailedException.class, () ->
                service.verify(issued.challenge().getId(), issued.code()));

        assertTrue(issued.challenge().isUsed());
    }

    @Test
    void fiveIncorrectAttemptsDisableChallenge() {
        IssuedChallenge issued = issueFor(usuario, List.of());
        when(challengeRepository.findByIdAndUsedFalse(issued.challenge().getId()))
                .thenAnswer(invocation -> issued.challenge().isUsed()
                        ? Optional.empty()
                        : Optional.of(issued.challenge()));
        String wrongCode = issued.code().equals("000000") ? "000001" : "000000";

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(TwoFactorVerificationFailedException.class, () ->
                    service.verify(issued.challenge().getId(), wrongCode));
        }

        assertEquals(5, issued.challenge().getAttempts());
        assertTrue(issued.challenge().isUsed());
        assertThrows(TwoFactorVerificationFailedException.class, () ->
                service.verify(issued.challenge().getId(), wrongCode));
    }

    @Test
    void challengeCodeIsBoundToItsOwnerAndChallengeId() {
        Usuario secondUser = user(12L, "second@example.com");
        IssuedChallenge first = issueFor(usuario, List.of());
        IssuedChallenge second = issueFor(secondUser, List.of());
        when(challengeRepository.findByIdAndUsedFalse(second.challenge().getId()))
                .thenReturn(Optional.of(second.challenge()));

        assertThrows(TwoFactorVerificationFailedException.class, () ->
                service.verify(second.challenge().getId(), first.code()));
        assertEquals(1, second.challenge().getAttempts());
    }

    @Test
    void newLoginInvalidatesPreviousActiveChallenge() {
        TwoFactorChallenge previous = new TwoFactorChallenge();
        previous.setUsuario(usuario);
        previous.setId(UUID.randomUUID());
        previous.setCodeHash("previous-hash");
        previous.setCreatedAt(Instant.now().minusSeconds(61));
        previous.setExpiresAt(Instant.now().plusSeconds(200));
        stubIssue(usuario, List.of(previous));
        when(challengeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(usuario.getId()))
                .thenReturn(Optional.of(previous));

        service.createChallenge(usuario);

        assertTrue(previous.isUsed());
        verify(challengeRepository).saveAll(List.of(previous));
    }

    @Test
    void consecutiveLoginWithinCooldownDoesNotSendOrReplaceOtp() {
        stubIssue(usuario, List.of());
        service.createChallenge(usuario);
        TwoFactorChallenge previous = latestSavedChallenge();
        when(challengeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(usuario.getId()))
                .thenReturn(Optional.of(previous));

        assertThrows(TwoFactorCooldownException.class, () -> service.createChallenge(usuario));

        assertFalse(previous.isUsed());
        verify(challengeRepository, times(1)).save(any(TwoFactorChallenge.class));
        verify(emailService, times(1)).sendTwoFactorCode(anyString(), anyString());
        verify(challengeRepository, times(1)).saveAll(any());
    }

    @Test
    void recentlyUsedChallengeStillEnforcesCooldown() {
        stubIssue(usuario, List.of());
        service.createChallenge(usuario);
        TwoFactorChallenge usedChallenge = latestSavedChallenge();
        usedChallenge.setUsed(true);
        when(challengeRepository.findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(usuario.getId()))
                .thenReturn(Optional.of(usedChallenge));

        assertThrows(TwoFactorCooldownException.class, () -> service.createChallenge(usuario));

        verify(emailService, times(1)).sendTwoFactorCode(anyString(), anyString());
        verify(challengeRepository, times(1)).saveAll(any());
    }

    private IssuedChallenge issueFor(Usuario target, List<TwoFactorChallenge> activeChallenges) {
        stubIssue(target, activeChallenges);
        TwoFactorRequiredResponse response = service.createChallenge(target);
        return new IssuedChallenge(
                latestSavedChallenge(),
                sentCodes.get(sentCodes.size() - 1)
        );
    }

    private void stubIssue(Usuario target, List<TwoFactorChallenge> activeChallenges) {
        when(usuarioRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        when(challengeRepository.findAllByUsuario_IdAndUsedFalse(target.getId()))
                .thenReturn(new ArrayList<>(activeChallenges));
    }

    private TwoFactorChallenge latestSavedChallenge() {
        return savedChallenges.get(savedChallenges.size() - 1);
    }

    private static Usuario user(Long id, String email) {
        Usuario user = new Usuario();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail(email);
        user.setEmailVerified(true);
        return user;
    }

    private record IssuedChallenge(TwoFactorChallenge challenge, String code) {
    }
}
