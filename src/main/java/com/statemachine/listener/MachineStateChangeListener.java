package com.statemachine.listener;

import com.statemachine.listener.event.MachineStateChangeEvent;

/**
 * Interface for listening to machine state change events.
 * Implementations can register themselves to receive notifications
 * when machine states change or context is updated.
 */
public interface MachineStateChangeListener {

    /**
     * Called when a machine state change event occurs.
     * 
     * @param event The machine state change event containing details about the change
     */
    void onStateChange(MachineStateChangeEvent event);

    /**
     * Returns a unique identifier for this listener.
     * Used for managing listener registration/unregistration.
     * 
     * @return Unique listener identifier
     */
    String getListenerId();
}
