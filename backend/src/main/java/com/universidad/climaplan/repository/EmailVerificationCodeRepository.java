package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.EmailVerificationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationCode> findFirstByUsuario_IdAndUsedFalseOrderByCreatedAtDescIdDesc(Long usuarioId);

    Optional<EmailVerificationCode> findFirstByUsuario_IdOrderByCreatedAtDescIdDesc(Long usuarioId);

    List<EmailVerificationCode> findAllByUsuario_IdAndUsedFalse(Long usuarioId);
}
