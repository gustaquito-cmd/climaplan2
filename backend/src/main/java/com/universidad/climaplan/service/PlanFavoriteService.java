package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanFavorite;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanFavoriteRepository;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlanFavoriteService {
    private final PlanFavoriteRepository favorites;
    private final PlanRepository plans;
    private final UsuarioRepository users;
    private final PlanService planService;

    public PlanFavoriteService(PlanFavoriteRepository favorites, PlanRepository plans,
                               UsuarioRepository users, PlanService planService) {
        this.favorites = favorites;
        this.plans = plans;
        this.users = users;
        this.planService = planService;
    }

    @Transactional(readOnly = true)
    public List<Long> list(String email, boolean admin) {
        Usuario user = users.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario autenticado"));
        Set<Long> visible = planService.obtenerTodos(email, admin).stream()
                .map(PlanResponse::id).collect(Collectors.toSet());
        return favorites.findPlanIdsByUsuarioId(user.getId()).stream()
                .filter(visible::contains).toList();
    }

    public void add(Long planId, String email, boolean admin) {
        Usuario user = lockUser(email);
        planService.obtenerPorId(planId, email, admin);
        if (favorites.findByUsuarioIdAndPlanId(user.getId(), planId).isEmpty()) {
            Plan plan = plans.findById(planId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el plan con ID " + planId));
            PlanFavorite favorite = new PlanFavorite();
            favorite.setPlan(plan);
            favorite.setUsuario(user);
            plan.addFavorite(favorite);
            favorites.save(favorite);
        }
    }

    public void remove(Long planId, String email, boolean admin) {
        Usuario user = lockUser(email);
        planService.obtenerPorId(planId, email, admin);
        favorites.findByUsuarioIdAndPlanId(user.getId(), planId).ifPresent(favorite -> {
            favorite.getPlan().removeFavorite(favorite);
            favorites.delete(favorite);
        });
    }

    private Usuario lockUser(String email) {
        return users.findByEmailForUpdate(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario autenticado"));
    }
}
