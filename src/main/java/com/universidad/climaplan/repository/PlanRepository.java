package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findAllByUsuarioId(Long usuarioId);

    Optional<Plan> findByIdAndUsuarioId(Long id, Long usuarioId);
}
