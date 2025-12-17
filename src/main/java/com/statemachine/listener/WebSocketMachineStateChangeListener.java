package com.statemachine.listener;

import com.statemachine.listener.event.MachineStateChangeEvent;
import com.statemachine.websocket.MachineStateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * WebSocket implementation of MachineStateChangeListener.
 * Bridges the listener mechanism to WebSocket broadcasting.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketMachineStateChangeListener implements MachineStateChangeListener {

    private final MachineStateWebSocketHandler webSocketHandler;
    private final MachineStateChangeListenerRegistry listenerRegistry;

    @PostConstruct
    public void init() {
        // Auto-register this listener on component initialization
        listenerRegistry.registerListener(this);
        log.info("WebSocket machine state change listener registered");
    }

    @Override
    public void onStateChange(MachineStateChangeEvent event) {
        try {
            // Convert the event to WebSocket message format
            MachineStateWebSocketHandler.MachineStateChangeMessage message = 
                new MachineStateWebSocketHandler.MachineStateChangeMessage(
                    event.getMachineId(),
                    event.getFromStateId(),
                    event.getToStateId(),
                    event.getTransitionId(),
                    event.getEventType() != null ? event.getEventType().name() : "UNKNOWN",
                    event.getMachineContext()
                );
            
            // Broadcast via WebSocket
            webSocketHandler.broadcastStateChange(message);
        } catch (Exception e) {
            log.error("Error broadcasting state change via WebSocket: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getListenerId() {
        return "websocket-listener";
    }
}
