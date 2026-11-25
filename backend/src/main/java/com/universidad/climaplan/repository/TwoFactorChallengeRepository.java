package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.TwoFactorChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TwoFactorChallenge> findByIdAndUsedFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TwoFactorChallenge> findAllByUsuario_IdAndUsedFalse(Long usuarioId);

    Optional<TwoFactorChallenge> findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(Long usuarioId);
}
