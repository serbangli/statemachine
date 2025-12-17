package com.statemachine.listener.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event representing a machine state change or context update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineStateChangeEvent {
    
    /**
     * The machine instance ID
     */
    private Long machineId;
    
    /**
     * The managed object ID (e.g., task ID)
     */
    private String managedObjectId;
    
    /**
     * The managed object type (e.g., REVIEW_TASK)
     */
    private String managedObjectType;
    
    /**
     * The machine definition ID
     */
    private String machineDefinitionId;
    
    /**
     * The state ID before the change (null for initial state)
     */
    private String fromStateId;
    
    /**
     * The state ID after the change
     */
    private String toStateId;
    
    /**
     * The transition ID that caused the change (null for context updates)
     */
    private String transitionId;
    
    /**
     * The type of event: STATE_CHANGED, CONTEXT_UPDATE, INITIAL
     */
    private EventType eventType;
    
    /**
     * The machine context at the time of the event
     */
    private Map<String, Object> machineContext;
    
    /**
     * Timestamp when the event occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * The role that triggered the change (if applicable)
     */
    private String role;
    
    /**
     * Event types for machine state changes
     */
    public enum EventType {
        /**
         * Initial state when machine is created
         */
        INITIAL,
        
        /**
         * State changed due to transition execution
         */
        STATE_CHANGED,
        
        /**
         * Context updated without state change
         */
        CONTEXT_UPDATE
    }
}
