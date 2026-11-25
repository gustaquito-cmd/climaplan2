package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.PlanShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanShareRepository extends JpaRepository<PlanShare, Long> {

    Optional<PlanShare> findByPlanId(Long planId);

    Optional<PlanShare> findByTokenAndActiveTrue(String token);
}
