package com.universidad.climaplan.service;

import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.EstadoUsuario;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanMember;
import com.universidad.climaplan.entity.PlanMemberStatus;
import com.universidad.climaplan.entity.Rol;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanFavoriteRepository;
import com.universidad.climaplan.repository.PlanMemberRepository;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({PlanFavoriteService.class, PlanService.class})
class PlanFavoriteServiceTest {
    @Autowired private PlanFavoriteService service;
    @Autowired private UsuarioRepository users;
    @Autowired private PlanRepository plans;
    @Autowired private PlanMemberRepository members;
    @Autowired private PlanFavoriteRepository favorites;

    @Test
    void favoritesArePerUserAndOperationsAreIdempotent() {
        Usuario owner = user("owner@example.com");
        Usuario member = user("member@example.com");
        Plan plan = plan(owner);
        addMember(plan, member, PlanMemberStatus.ACCEPTED);

        service.add(plan.getId(), owner.getEmail(), false);
        service.add(plan.getId(), owner.getEmail(), false);
        service.add(plan.getId(), member.getEmail(), false);
        assertEquals(2, favorites.count());
        assertEquals(List.of(plan.getId()), service.list(owner.getEmail(), false));
        assertEquals(List.of(plan.getId()), service.list(member.getEmail(), false));

        service.remove(plan.getId(), member.getEmail(), false);
        service.remove(plan.getId(), member.getEmail(), false);
        assertTrue(service.list(member.getEmail(), false).isEmpty());
        assertEquals(List.of(plan.getId()), service.list(owner.getEmail(), false));
    }

    @Test
    void unauthorizedOrFormerMembersCannotAccessFavorite() {
        Usuario owner = user("owner@example.com");
        Usuario member = user("member@example.com");
        Plan plan = plan(owner);
        PlanMember participation = addMember(plan, member, PlanMemberStatus.ACCEPTED);
        service.add(plan.getId(), member.getEmail(), false);

        members.delete(participation);
        members.flush();
        assertTrue(service.list(member.getEmail(), false).isEmpty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.add(plan.getId(), member.getEmail(), false));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.remove(plan.getId(), member.getEmail(), false));
    }

    @Test
    void deletingPlanRemovesItsFavorites() {
        Usuario owner = user("owner@example.com");
        Plan plan = plan(owner);
        service.add(plan.getId(), owner.getEmail(), false);

        plans.delete(plan);
        plans.flush();

        assertEquals(0, favorites.count());
    }

    @Test
    void plansInTheSameCityHaveIndependentFavorites() {
        Usuario owner = user("owner@example.com");
        Plan first = plan(owner);
        Plan second = plan(owner);

        service.add(first.getId(), owner.getEmail(), false);

        assertEquals(List.of(first.getId()), service.list(owner.getEmail(), false));
        assertTrue(favorites.findByUsuarioIdAndPlanId(owner.getId(), second.getId()).isEmpty());
    }

    private Usuario user(String email) {
        Usuario user = new Usuario();
        user.setNombre("Test");
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setEstado(EstadoUsuario.ACTIVO);
        user.setRol(Rol.ROLE_USER);
        return users.save(user);
    }

    private Plan plan(Usuario owner) {
        Plan plan = new Plan();
        plan.setUsuario(owner);
        plan.setTitulo("Plan");
        plan.setCiudad("Tuxtla");
        plan.setTipoActividad("Viaje");
        plan.setFecha(LocalDate.of(2026, 12, 1));
        plan.setEstado(EstadoPlan.PENDIENTE);
        return plans.save(plan);
    }

    private PlanMember addMember(Plan plan, Usuario user, PlanMemberStatus status) {
        PlanMember member = new PlanMember();
        member.setPlan(plan);
        member.setUsuario(user);
        member.setStatus(status);
        member.setInvitedAt(LocalDateTime.now());
        return members.save(member);
    }
}
