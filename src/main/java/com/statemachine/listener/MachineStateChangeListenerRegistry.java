package com.statemachine.listener;

import com.statemachine.listener.event.MachineStateChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing machine state change listeners.
 * Provides thread-safe registration and notification of listeners.
 */
@Service
@Slf4j
public class MachineStateChangeListenerRegistry {

    private final Set<MachineStateChangeListener> listeners = 
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Registers a listener to receive machine state change events.
     * 
     * @param listener The listener to register
     * @return true if the listener was added, false if it was already registered
     */
    public boolean registerListener(MachineStateChangeListener listener) {
        if (listener == null) {
            log.warn("Attempted to register null listener");
            return false;
        }
        
        boolean added = listeners.add(listener);
        if (added) {
            log.debug("Registered machine state change listener: {}", listener.getListenerId());
        } else {
            log.debug("Listener already registered: {}", listener.getListenerId());
        }
        return added;
    }

    /**
     * Unregisters a listener from receiving machine state change events.
     * 
     * @param listener The listener to unregister
     * @return true if the listener was removed, false if it was not registered
     */
    public boolean unregisterListener(MachineStateChangeListener listener) {
        if (listener == null) {
            return false;
        }
        
        boolean removed = listeners.remove(listener);
        if (removed) {
            log.debug("Unregistered machine state change listener: {}", listener.getListenerId());
        }
        return removed;
    }

    /**
     * Unregisters a listener by its ID.
     * 
     * @param listenerId The ID of the listener to unregister
     * @return true if a listener was removed, false otherwise
     */
    public boolean unregisterListenerById(String listenerId) {
        return listeners.removeIf(listener -> listener.getListenerId().equals(listenerId));
    }

    /**
     * Notifies all registered listeners about a machine state change event.
     * This method is thread-safe and handles exceptions from individual listeners
     * without affecting other listeners.
     * 
     * @param event The machine state change event
     */
    public void notifyListeners(MachineStateChangeEvent event) {
        if (event == null) {
            log.warn("Attempted to notify listeners with null event");
            return;
        }

        log.debug("Notifying {} listeners about machine state change: machineId={}, fromState={}, toState={}, type={}",
                listeners.size(), event.getMachineId(), event.getFromStateId(), 
                event.getToStateId(), event.getEventType());

        for (MachineStateChangeListener listener : listeners) {
            try {
                listener.onStateChange(event);
            } catch (Exception e) {
                log.error("Error notifying listener {} about state change: {}", 
                        listener.getListenerId(), e.getMessage(), e);
                // Continue notifying other listeners even if one fails
            }
        }
    }

    /**
     * Gets the number of registered listeners.
     * 
     * @return The number of registered listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Gets an unmodifiable view of all registered listeners.
     * 
     * @return Set of registered listeners
     */
    public Set<MachineStateChangeListener> getListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Clears all registered listeners.
     * Use with caution - typically only for testing or shutdown.
     */
    public void clearListeners() {
        log.warn("Clearing all machine state change listeners");
        listeners.clear();
    }
}
