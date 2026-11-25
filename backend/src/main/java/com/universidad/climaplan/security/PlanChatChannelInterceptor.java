package com.universidad.climaplan.security;

import com.universidad.climaplan.exception.RecursoNoEncontradoException;
import com.universidad.climaplan.service.PlanService;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanChatChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ERROR_DESTINATION = "/user/queue/chat-errors";
    private static final Pattern SEND_DESTINATION = Pattern.compile("^/app/plans/(\\d+)/chat$");
    private static final Pattern SUBSCRIBE_DESTINATION = Pattern.compile("^/topic/plans/(\\d+)/chat$");

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PlanService planService;
    private final SimpUserRegistry userRegistry;
    private final boolean outbound;

    public PlanChatChannelInterceptor(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            PlanService planService,
            SimpUserRegistry userRegistry,
            boolean outbound
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.planService = planService;
        this.userRegistry = userRegistry;
        this.outbound = outbound;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        if (outbound) return filterOutbound(message, accessor);
        return validateInbound(message, accessor);
    }

    private Message<?> validateInbound(Message<?> message, StompHeaderAccessor accessor) {
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader(AUTHORIZATION_HEADER)));
        } else if (command == StompCommand.SEND) {
            authorizeDestination(accessor.getDestination(), SEND_DESTINATION, accessor.getUser());
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (USER_ERROR_DESTINATION.equals(destination)) return message;
            authorizeDestination(destination, SUBSCRIBE_DESTINATION, accessor.getUser());
        }
        return message;
    }

    private Message<?> filterOutbound(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getCommand() != StompCommand.MESSAGE) return message;
        Matcher matcher = SUBSCRIBE_DESTINATION.matcher(
                accessor.getDestination() == null ? "" : accessor.getDestination()
        );
        if (!matcher.matches()) return message;

        long planId = parsePlanId(matcher.group(1));
        String email = getSessionUsername(accessor.getSessionId());
        if (email == null || !hasPlanAccess(planId, email)) return null;
        return message;
    }

    private String getSessionUsername(String sessionId) {
        if (sessionId == null) return null;
        for (SimpUser user : userRegistry.getUsers()) {
            if (user.getSession(sessionId) != null) return user.getName();
        }
        return null;
    }

    private Authentication authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessageDeliveryException("No se pudo autenticar la conexión del chat");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new MessageDeliveryException("No se pudo autenticar la conexión del chat");
        }

        try {
            String email = jwtService.extraerEmail(token);
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (!jwtService.esTokenValido(token, user)) {
                throw new MessageDeliveryException("No se pudo autenticar la conexión del chat");
            }
            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            throw new MessageDeliveryException("No se pudo autenticar la conexión del chat");
        }
    }

    private void authorizeDestination(
            String destination,
            Pattern pattern,
            Principal principal
    ) {
        Matcher matcher = pattern.matcher(destination == null ? "" : destination);
        if (!matcher.matches() || principal == null) {
            throw new MessageDeliveryException("Destino de chat no autorizado");
        }
        long planId = parsePlanId(matcher.group(1));
        try {
            planService.obtenerPlanLegible(planId, principal.getName());
        } catch (RecursoNoEncontradoException exception) {
            throw new MessageDeliveryException("No tienes acceso al chat de este plan");
        }
    }

    private boolean hasPlanAccess(long planId, String email) {
        try {
            planService.obtenerPlanLegible(planId, email);
            return true;
        } catch (RecursoNoEncontradoException exception) {
            return false;
        }
    }

    private long parsePlanId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new MessageDeliveryException("Destino de chat inválido");
        }
    }
}
