package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.PlanMessageRequest;
import com.universidad.climaplan.dto.PlanMessageResponse;
import com.universidad.climaplan.service.PlanMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.Instant;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanChatWebSocketControllerTest {

    @Test
    void persisteElMensajeAntesDePublicarloAlTopicDelPlan() {
        PlanMessageService service = mock(PlanMessageService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        PlanChatWebSocketController controller =
                new PlanChatWebSocketController(service, messagingTemplate);
        Principal principal = () -> "member@example.com";
        PlanMessageRequest request = new PlanMessageRequest("Nos vemos a las siete");
        PlanMessageResponse persisted = new PlanMessageResponse(
                501L,
                91L,
                9L,
                "Carlos",
                request.content(),
                Instant.parse("2026-11-24T18:00:00Z")
        );
        when(service.enviar(91L, request, "member@example.com")).thenReturn(persisted);

        controller.enviar(91L, request, principal);

        var inOrder = inOrder(service, messagingTemplate);
        inOrder.verify(service).enviar(91L, request, "member@example.com");
        inOrder.verify(messagingTemplate).convertAndSend("/topic/plans/91/chat", persisted);
    }
}
