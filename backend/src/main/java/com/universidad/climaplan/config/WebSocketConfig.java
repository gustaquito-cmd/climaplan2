package com.universidad.climaplan.config;

import com.universidad.climaplan.security.JwtService;
import com.universidad.climaplan.security.PlanChatChannelInterceptor;
import com.universidad.climaplan.service.PlanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> allowedOrigins;
    private final PlanChatChannelInterceptor inboundInterceptor;
    private final PlanChatChannelInterceptor outboundInterceptor;

    public WebSocketConfig(
            @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}") List<String> allowedOrigins,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            PlanService planService,
            SimpUserRegistry userRegistry
    ) {
        this.allowedOrigins = allowedOrigins;
        this.inboundInterceptor =
                new PlanChatChannelInterceptor(
                        jwtService, userDetailsService, planService, userRegistry, false);
        this.outboundInterceptor =
                new PlanChatChannelInterceptor(
                        jwtService, userDetailsService, planService, userRegistry, true);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(inboundInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(outboundInterceptor);
    }
}
