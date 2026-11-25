package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.PlanFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanFavoriteRepository extends JpaRepository<PlanFavorite, Long> {
    Optional<PlanFavorite> findByUsuarioIdAndPlanId(Long usuarioId, Long planId);

    @Query("select favorite.plan.id from PlanFavorite favorite where favorite.usuario.id = :usuarioId")
    List<Long> findPlanIdsByUsuarioId(@Param("usuarioId") Long usuarioId);
}
