package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.PlanMember;
import com.universidad.climaplan.entity.PlanMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanMemberRepository extends JpaRepository<PlanMember, Long> {

    List<PlanMember> findAllByPlanIdOrderByInvitedAtAsc(Long planId);

    List<PlanMember> findAllByUsuarioIdAndStatusOrderByInvitedAtDesc(
            Long usuarioId,
            PlanMemberStatus status
    );

    Optional<PlanMember> findByPlanIdAndUsuarioId(Long planId, Long usuarioId);

    Optional<PlanMember> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByPlanIdAndUsuarioIdAndStatus(
            Long planId,
            Long usuarioId,
            PlanMemberStatus status
    );
}
