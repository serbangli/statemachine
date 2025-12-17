# Machine State Change Listener Mechanism

This package provides a listener mechanism for machine state changes, allowing components to be notified when machine states change or context is updated.

## Overview

The listener mechanism consists of:

1. **MachineStateChangeListener** - Interface that components implement to receive state change notifications
2. **MachineStateChangeEvent** - Event object containing details about the state change
3. **MachineStateChangeListenerRegistry** - Service that manages listener registration and notification
4. **WebSocketMachineStateChangeListener** - Built-in listener that broadcasts events via WebSocket
5. **LoggingMachineStateChangeListener** - Built-in listener that logs all state changes

## Usage

### Implementing a Custom Listener

```java
@Component
public class MyCustomListener implements MachineStateChangeListener {
    
    private final MachineStateChangeListenerRegistry listenerRegistry;
    
    public MyCustomListener(MachineStateChangeListenerRegistry listenerRegistry) {
        this.listenerRegistry = listenerRegistry;
    }
    
    @PostConstruct
    public void init() {
        listenerRegistry.registerListener(this);
    }
    
    @Override
    public void onStateChange(MachineStateChangeEvent event) {
        // Handle the state change event
        System.out.println("Machine " + event.getMachineId() + 
                          " changed from " + event.getFromStateId() + 
                          " to " + event.getToStateId());
    }
    
    @Override
    public String getListenerId() {
        return "my-custom-listener";
    }
}
```

### Event Types

The `MachineStateChangeEvent` supports three event types:

- **INITIAL** - Fired when a machine instance is first created
- **STATE_CHANGED** - Fired when a transition is executed and the state changes
- **CONTEXT_UPDATE** - Fired when the machine context is updated without a state change

### Event Information

Each `MachineStateChangeEvent` contains:

- `machineId` - The machine instance ID
- `managedObjectId` - The managed object ID (e.g., task ID)
- `managedObjectType` - The managed object type (e.g., REVIEW_TASK)
- `machineDefinitionId` - The machine definition ID
- `fromStateId` - The previous state (null for INITIAL events)
- `toStateId` - The new state
- `transitionId` - The transition that caused the change (null for context updates)
- `eventType` - The type of event
- `machineContext` - A snapshot of the machine context at the time of the event
- `timestamp` - When the event occurred
- `role` - The role that triggered the change (if applicable)

### Manual Registration/Unregistration

```java
@Autowired
private MachineStateChangeListenerRegistry listenerRegistry;

// Register a listener
MyListener listener = new MyListener();
listenerRegistry.registerListener(listener);

// Unregister a listener
listenerRegistry.unregisterListener(listener);

// Or unregister by ID
listenerRegistry.unregisterListenerById("my-listener-id");
```

## Built-in Listeners

### WebSocket Listener

The `WebSocketMachineStateChangeListener` automatically broadcasts all state change events to connected WebSocket clients. This is registered automatically and requires no additional configuration.

### Logging Listener

The `LoggingMachineStateChangeListener` logs all state change events at INFO level. This is useful for debugging and monitoring.

## Thread Safety

The listener registry is thread-safe. Listeners are notified synchronously in the same thread that triggers the state change. If a listener throws an exception, it does not affect other listeners.

## Best Practices

1. **Keep listeners lightweight** - Listeners should perform quick operations. For heavy processing, consider using async mechanisms.

2. **Handle exceptions** - Always handle exceptions in your listener implementation to prevent affecting other listeners.

3. **Use @PostConstruct for auto-registration** - This ensures listeners are registered when the Spring context is initialized.

4. **Unregister when done** - If you create listeners dynamically, remember to unregister them when they're no longer needed.

## Example Use Cases

- **Audit Logging** - Log all state changes to an audit system
- **Notification System** - Send emails or push notifications when certain states are reached
- **Metrics Collection** - Track state transition metrics
- **Integration** - Sync state changes with external systems
- **Caching** - Invalidate caches when states change
