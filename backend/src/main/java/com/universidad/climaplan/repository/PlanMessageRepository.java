package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.PlanMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanMessageRepository extends JpaRepository<PlanMessage, Long> {

    Page<PlanMessage> findAllByPlanIdOrderBySentAtDescIdDesc(Long planId, Pageable pageable);
}
