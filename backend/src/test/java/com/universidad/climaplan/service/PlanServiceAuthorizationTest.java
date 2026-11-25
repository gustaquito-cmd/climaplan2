package com.universidad.climaplan.service;

import com.universidad.climaplan.dto.PlanCreateRequest;
import com.universidad.climaplan.dto.PlanOwnership;
import com.universidad.climaplan.dto.PlanUpdateRequest;
import com.universidad.climaplan.dto.weather.WeatherDataType;
import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.entity.PlanMember;
import com.universidad.climaplan.entity.PlanMemberStatus;
import com.universidad.climaplan.entity.Usuario;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.repository.PlanRepository;
import com.universidad.climaplan.repository.PlanMemberRepository;
import com.universidad.climaplan.repository.UsuarioRepository;
import com.universidad.climaplan.service.weather.CondicionesMeteorologicas;
import com.universidad.climaplan.service.weather.OpenWeatherService;
import com.universidad.climaplan.service.weather.PlanWeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class PlanServiceAuthorizationTest {

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final PlanMemberRepository planMemberRepository = mock(PlanMemberRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PlanService planService =
            new PlanService(planRepository, planMemberRepository, usuarioRepository);

    @Test
    void usuarioNoPuedeConsultarPlanDeOtroUsuario() {
        Usuario usuarioAutenticado = new Usuario();
        ReflectionTestUtils.setField(usuarioAutenticado, "id", 7L);
        when(usuarioRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(usuarioAutenticado));
        when(planRepository.findByIdAndUsuarioId(91L, 7L))
                .thenReturn(Optional.empty());
        when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planService.obtenerPorId(91L, "ana@example.com", false)
        );

        verify(planRepository).findByIdAndUsuarioId(91L, 7L);
        verify(planRepository, never()).findById(91L);
    }

    @Test
    void crearSiempreGuardaElPlanComoPendiente() {
        Usuario propietario = usuario(7L);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(propietario));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PlanCreateRequest request = new PlanCreateRequest(
                "Caminata",
                "Tuxtla Gutiérrez",
                LocalDate.now().plusDays(3),
                "Senderismo",
                null
        );

        var response = planService.crear(request, "ana@example.com");

        assertEquals(EstadoPlan.PENDIENTE, response.estado());
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void actualizarPermiteCambiarElEstadoDelPlan() {
        Usuario propietario = usuario(7L);
        Plan plan = new Plan();
        plan.setUsuario(propietario);
        plan.setEstado(EstadoPlan.PENDIENTE);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(propietario));
        when(planRepository.findByIdAndUsuarioId(91L, 7L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PlanUpdateRequest request = new PlanUpdateRequest(
                "Caminata",
                "Tuxtla Gutiérrez",
                LocalDate.now().plusDays(3),
                "Senderismo",
                null,
                EstadoPlan.COMPLETADO
        );

        var response = planService.actualizar(91L, request, "ana@example.com", false);

        assertEquals(EstadoPlan.COMPLETADO, response.estado());
        verify(planRepository).save(plan);
    }

    @Test
    void acceptedMemberPuedeConsultarPlanYResponseIndicaMember() {
        Usuario propietario = usuario(7L);
        Usuario memberUser = usuario(9L);
        Plan plan = planPropietario(propietario);
        PlanMember member = member(plan, memberUser, PlanMemberStatus.ACCEPTED);
        when(usuarioRepository.findByEmail("member@example.com")).thenReturn(Optional.of(memberUser));
        when(planRepository.findByIdAndUsuarioId(91L, 9L)).thenReturn(Optional.empty());
        when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 9L)).thenReturn(Optional.of(member));

        var response = planService.obtenerPorId(91L, "member@example.com", false);

        assertEquals(PlanOwnership.MEMBER, response.ownership());
        assertEquals("Caminata", response.titulo());
    }

    @Test
    void pendingYRejectedMembersNoPuedenConsultarElPlan() {
        Usuario propietario = usuario(7L);
        Usuario invitedUser = usuario(9L);
        Plan plan = planPropietario(propietario);
        when(usuarioRepository.findByEmail("member@example.com")).thenReturn(Optional.of(invitedUser));
        when(planRepository.findByIdAndUsuarioId(91L, 9L)).thenReturn(Optional.empty());

        for (PlanMemberStatus status : List.of(PlanMemberStatus.PENDING, PlanMemberStatus.REJECTED)) {
            when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 9L))
                    .thenReturn(Optional.of(member(plan, invitedUser, status)));
            assertThrows(
                    RecursoNoEncontradoException.class,
                    () -> planService.obtenerPorId(91L, "member@example.com", false)
            );
        }
    }

    @Test
    void acceptedMemberNoPuedeEditarNiEliminarAunqueAutenticado() {
        Usuario invitedUser = usuario(9L);
        when(usuarioRepository.findByEmail("member@example.com")).thenReturn(Optional.of(invitedUser));
        when(planRepository.findByIdAndUsuarioId(91L, 9L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planService.actualizar(
                        91L,
                        new PlanUpdateRequest("Caminata", "Tuxtla", LocalDate.now(), "Paseo", null, null),
                        "member@example.com",
                        false
                )
        );
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planService.eliminar(91L, "member@example.com", false)
        );

        verify(planRepository, never()).save(any(Plan.class));
        verify(planRepository, never()).delete(any(Plan.class));
    }

    @Test
    void administradorNoPropietarioTampocoPuedeModificarNiEliminar() {
        Usuario administrator = usuario(12L);
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
        when(planRepository.findByIdAndUsuarioId(91L, 12L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planService.actualizar(
                        91L,
                        new PlanUpdateRequest("Caminata", "Tuxtla", LocalDate.now(), "Paseo", null, null),
                        "admin@example.com",
                        true
                )
        );
        assertThrows(
                RecursoNoEncontradoException.class,
                () -> planService.eliminar(91L, "admin@example.com", true)
        );

        verify(planRepository, never()).save(any(Plan.class));
        verify(planRepository, never()).delete(any(Plan.class));
    }

    @Test
    void listadoIncluyePlanesPropiosYParticipacionesAceptadasConOwnership() {
        Usuario usuario = usuario(7L);
        Plan own = planPropietario(usuario);
        Plan shared = planPropietario(usuario(8L));
        ReflectionTestUtils.setField(shared, "id", 92L);
        PlanMember accepted = member(shared, usuario, PlanMemberStatus.ACCEPTED);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(planRepository.findAllByUsuarioId(7L)).thenReturn(List.of(own));
        when(planMemberRepository.findAllByUsuarioIdAndStatusOrderByInvitedAtDesc(
                7L,
                PlanMemberStatus.ACCEPTED
        )).thenReturn(List.of(accepted));

        var response = planService.obtenerTodos("ana@example.com", false);

        assertEquals(2, response.size());
        assertEquals(PlanOwnership.OWNER, response.getFirst().ownership());
        assertEquals(PlanOwnership.MEMBER, response.getLast().ownership());
    }

    @Test
    void acceptedMemberPuedeConsultarWeatherCheck() {
        Usuario invitedUser = usuario(9L);
        Plan plan = planPropietario(usuario(7L));
        plan.setFecha(LocalDate.now());
        PlanMember accepted = member(plan, invitedUser, PlanMemberStatus.ACCEPTED);
        when(usuarioRepository.findByEmail("member@example.com")).thenReturn(Optional.of(invitedUser));
        when(planRepository.findByIdAndUsuarioId(91L, 9L)).thenReturn(Optional.empty());
        when(planMemberRepository.findByPlanIdAndUsuarioId(91L, 9L)).thenReturn(Optional.of(accepted));
        OpenWeatherService openWeatherService = mock(OpenWeatherService.class);
        when(openWeatherService.consultarClimaDetallado("Tuxtla Gutiérrez"))
                .thenReturn(new CondicionesMeteorologicas(
                        new WeatherResponse(
                                "Tuxtla Gutiérrez",
                                "MX",
                                16.75,
                                -93.12,
                                25,
                                25,
                                50,
                                "cielo claro",
                                3
                        ),
                        800,
                        "Clear"
                ));
        PlanWeatherService weatherService = new PlanWeatherService(planService, openWeatherService);

        var response = weatherService.verificarPlan(91L, "member@example.com", false);

        assertEquals(WeatherDataType.CURRENT, response.weatherDataType());
        verify(openWeatherService).consultarClimaDetallado("Tuxtla Gutiérrez");
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private static Plan planPropietario(Usuario propietario) {
        Plan plan = new Plan();
        ReflectionTestUtils.setField(plan, "id", 91L);
        plan.setTitulo("Caminata");
        plan.setCiudad("Tuxtla Gutiérrez");
        plan.setFecha(LocalDate.now().plusDays(3));
        plan.setTipoActividad("Senderismo");
        plan.setEstado(EstadoPlan.PENDIENTE);
        plan.setUsuario(propietario);
        return plan;
    }

    private static PlanMember member(Plan plan, Usuario usuario, PlanMemberStatus status) {
        PlanMember member = new PlanMember();
        member.setPlan(plan);
        member.setUsuario(usuario);
        member.setStatus(status);
        return member;
    }
}
