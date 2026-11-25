package com.universidad.climaplan.security;

import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.service.PlanService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanChatChannelInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final PlanService planService = mock(PlanService.class);
    private final SimpUserRegistry userRegistry = mock(SimpUserRegistry.class);
    private final PlanChatChannelInterceptor inbound =
            new PlanChatChannelInterceptor(
                    jwtService, userDetailsService, planService, userRegistry, false);

    private final UserDetails user = User.withUsername("owner@example.com")
            .password("unused")
            .authorities("ROLE_USER")
            .build();

    @BeforeEach
    void setUp() {
        when(jwtService.extraerEmail("valid-token")).thenReturn("owner@example.com");
        when(userDetailsService.loadUserByUsername("owner@example.com")).thenReturn(user);
        when(jwtService.esTokenValido("valid-token", user)).thenReturn(true);
    }

    @Test
    void connectValidoAutenticaLaSesionConJwtExistente() {
        Message<?> result = inbound.preSend(message(StompCommand.CONNECT, null, "Bearer valid-token"), null);

        assertNotNull(StompHeaderAccessor.wrap(result).getUser());
        verify(jwtService).esTokenValido("valid-token", user);
    }

    @Test
    void connectInvalidoYExpiradoSonRechazados() {
        when(jwtService.extraerEmail("bad-token")).thenThrow(new JwtException("invalid"));
        assertThrows(
                MessageDeliveryException.class,
                () -> inbound.preSend(message(StompCommand.CONNECT, null, "Bearer bad-token"), null)
        );

        when(jwtService.extraerEmail("expired-token")).thenReturn("owner@example.com");
        when(jwtService.esTokenValido("expired-token", user)).thenReturn(false);
        assertThrows(
                MessageDeliveryException.class,
                () -> inbound.preSend(message(StompCommand.CONNECT, null, "Bearer expired-token"), null)
        );
    }

    @Test
    void connectRequiereAuthorizationBearerEnHeaderStomp() {
        assertThrows(
                MessageDeliveryException.class,
                () -> inbound.preSend(message(StompCommand.CONNECT, null, null), null)
        );
        verify(jwtService, never()).extraerEmail(any());
    }

    @Test
    void sendYSuscribeCompruebanAccesoAlPlanAntesDePermitirElDestino() {
        when(planService.obtenerPlanLegible(91L, "owner@example.com")).thenReturn(plan());

        Message<?> send = inbound.preSend(
                message(StompCommand.SEND, "/app/plans/91/chat", null, "owner@example.com"),
                null
        );
        Message<?> subscribe = inbound.preSend(
                message(StompCommand.SUBSCRIBE, "/topic/plans/91/chat", null, "owner@example.com"),
                null
        );

        assertNotNull(send);
        assertNotNull(subscribe);
        verify(planService, org.mockito.Mockito.times(2))
                .obtenerPlanLegible(91L, "owner@example.com");
    }

    @Test
    void sendYSuscribeDeUsuarioAjenoSonRechazados() {
        when(planService.obtenerPlanLegible(91L, "stranger@example.com"))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el plan"));

        assertThrows(
                MessageDeliveryException.class,
                () -> inbound.preSend(
                        message(StompCommand.SEND, "/app/plans/91/chat", null, "stranger@example.com"),
                        null
                )
        );
        assertThrows(
                MessageDeliveryException.class,
                () -> inbound.preSend(
                        message(StompCommand.SUBSCRIBE, "/topic/plans/91/chat", null, "stranger@example.com"),
                        null
                )
        );
    }

    @Test
    void destinosInvalidosSeRechazanSinConsultarPlan() {
        assertThrows(
                MessageDeliveryException.class,
                () -> inbound.preSend(
                        message(StompCommand.SEND, "/app/plans/not-a-number/chat", null, "owner@example.com"),
                        null
                )
        );
        assertThrows(
                MessageDeliveryException.class,
                () -> inbound.preSend(
                        message(StompCommand.SUBSCRIBE, "/topic/plans/91/messages", null, "owner@example.com"),
                        null
                )
        );
        verify(planService, never()).obtenerPlanLegible(any(), any());
    }

    @Test
    void outboundNoEntregaMensajesAUnaSesionRevocada() {
        PlanChatChannelInterceptor outbound =
                new PlanChatChannelInterceptor(
                        jwtService, userDetailsService, planService, userRegistry, true);
        SimpUser sessionUser = mock(SimpUser.class);
        when(sessionUser.getName()).thenReturn("removed@example.com");
        when(sessionUser.getSession("session-1")).thenReturn(mock(SimpSession.class));
        when(userRegistry.getUsers()).thenReturn(Set.of(sessionUser));
        when(planService.obtenerPlanLegible(91L, "removed@example.com"))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el plan"));

        Message<?> result = outbound.preSend(
                message(StompCommand.MESSAGE, "/topic/plans/91/chat", null, null, "session-1"),
                null
        );

        assertNull(result);
    }

    @Test
    void outboundResuelveAlDestinatarioPorSesionAunqueElMensajeBrokerNoTengaPrincipal() {
        PlanChatChannelInterceptor outbound =
                new PlanChatChannelInterceptor(
                        jwtService, userDetailsService, planService, userRegistry, true);
        SimpUser sessionUser = mock(SimpUser.class);
        when(sessionUser.getName()).thenReturn("owner@example.com");
        when(sessionUser.getSession("session-2")).thenReturn(mock(SimpSession.class));
        when(userRegistry.getUsers()).thenReturn(Set.of(sessionUser));
        when(planService.obtenerPlanLegible(91L, "owner@example.com")).thenReturn(plan());

        Message<?> result = outbound.preSend(
                message(StompCommand.MESSAGE, "/topic/plans/91/chat", null, null, "session-2"),
                null
        );

        assertNotNull(result);
        verify(planService).obtenerPlanLegible(91L, "owner@example.com");
    }

    private Message<byte[]> message(StompCommand command, String destination, String authorization) {
        return message(command, destination, authorization, null);
    }

    private Message<byte[]> message(
            StompCommand command,
            String destination,
            String authorization,
            String principal
    ) {
        return message(command, destination, authorization, principal, null);
    }

    private Message<byte[]> message(
            StompCommand command,
            String destination,
            String authorization,
            String principal,
            String sessionId
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (authorization != null) accessor.addNativeHeader("Authorization", authorization);
        if (principal != null) accessor.setUser(() -> principal);
        if (sessionId != null) accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Plan plan() {
        Plan plan = new Plan();
        ReflectionTestUtils.setField(plan, "id", 91L);
        return plan;
    }
}
