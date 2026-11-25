package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.PlanChatErrorResponse;
import com.universidad.climaplan.dto.PlanMessageRequest;
import com.universidad.climaplan.service.PlanMessageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Controller
@Validated
public class PlanChatWebSocketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanChatWebSocketController.class);
    private final PlanMessageService planMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    public PlanChatWebSocketController(
            PlanMessageService planMessageService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.planMessageService = planMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/plans/{planId}/chat")
    public void enviar(
            @DestinationVariable Long planId,
            @Valid @Payload PlanMessageRequest request,
            Principal principal
    ) {
        var savedMessage = planMessageService.enviar(planId, request, principal.getName());
        messagingTemplate.convertAndSend("/topic/plans/" + planId + "/chat", savedMessage);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/chat-errors")
    public PlanChatErrorResponse manejarError(Exception exception) {
        LOGGER.error("Error procesando un mensaje de chat", exception);
        return new PlanChatErrorResponse("No se pudo enviar el mensaje. Revisa el contenido e inténtalo nuevamente.");
    }
}
