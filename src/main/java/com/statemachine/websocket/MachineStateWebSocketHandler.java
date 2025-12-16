package com.statemachine.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class MachineStateWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Set<WebSocketSession> sessions =
            Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("WebSocket connection closed: {}", session.getId());
    }

    /**
     * Broadcasts a machine state change event to all connected clients.
     */
    public void broadcastStateChange(MachineStateChangeMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(payload);

            synchronized (sessions) {
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to broadcast machine state change", e);
        }
    }

    /**
     * Simple DTO for messages sent over WebSocket.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MachineStateChangeMessage {
        private Long machineId;
        private String fromStateId;
        private String toStateId;
        private String transitionId;
        private String type;
        private Map<String, Object> machineContext;
    }
}

