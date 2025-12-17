package com.statemachine.listener;

import com.statemachine.listener.event.MachineStateChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Example logging listener that logs all machine state changes.
 * This demonstrates how to implement a custom listener.
 */
@Component
@Slf4j
public class LoggingMachineStateChangeListener implements MachineStateChangeListener {

    private final MachineStateChangeListenerRegistry listenerRegistry;

    public LoggingMachineStateChangeListener(MachineStateChangeListenerRegistry listenerRegistry) {
        this.listenerRegistry = listenerRegistry;
    }

    @PostConstruct
    public void init() {
        // Auto-register this listener on component initialization
        listenerRegistry.registerListener(this);
        log.info("Logging machine state change listener registered");
    }

    @Override
    public void onStateChange(MachineStateChangeEvent event) {
        log.info("Machine State Change Event - MachineId: {}, FromState: {}, ToState: {}, " +
                "TransitionId: {}, EventType: {}, Role: {}, Timestamp: {}",
                event.getMachineId(),
                event.getFromStateId(),
                event.getToStateId(),
                event.getTransitionId(),
                event.getEventType(),
                event.getRole(),
                event.getTimestamp());
        
        if (event.getManagedObjectId() != null) {
            log.info("Managed Object - ID: {}, Type: {}", 
                    event.getManagedObjectId(), event.getManagedObjectType());
        }
    }

    @Override
    public String getListenerId() {
        return "logging-listener";
    }
}
