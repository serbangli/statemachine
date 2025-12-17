package com.statemachine.service;

import com.statemachine.domain.entity.MachineEntity;
import com.statemachine.domain.entity.MachineDefinitionEntity;
import com.statemachine.domain.entity.MachineHistoryEntity;
import com.statemachine.domain.model.MachineDefinition;
import com.statemachine.domain.model.Transition;
import com.statemachine.repository.MachineRepository;
import com.statemachine.repository.MachineHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;
import com.statemachine.listener.MachineStateChangeListenerRegistry;
import com.statemachine.listener.event.MachineStateChangeEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class MachineService {

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private MachineHistoryRepository machineHistoryRepository;

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    @Autowired
    private ConditionEvaluator conditionEvaluator;

    @Autowired
    private MachineStateChangeListenerRegistry listenerRegistry;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public MachineEntity createMachineInstance(String machineDefinitionId, Map<String, Object> initialContext, String managedObjectId, String managedObjectType) {
        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(machineDefinitionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Machine definition not found: " + machineDefinitionId));

        MachineDefinition model = machineDefinitionService.convertToModel(definition);

        if (model.getStartState() == null) {
            throw new IllegalStateException("Machine definition must have a start state");
        }

        MachineEntity machine = new MachineEntity();
        machine.setMachineDefinitionId(machineDefinitionId);
        machine.setCurrentStateId(model.getStartState().getId());
        machine.setContext(initialContext != null ? new HashMap<>(initialContext) : new HashMap<>());
        machine.setManagedObjectId(managedObjectId);
        machine.setManagedObjectType(managedObjectType);
        machine = machineRepository.save(machine);

        // Record initial state in history
        recordHistory(machine.getId(), null, model.getStartState().getId(),
                "INITIAL", new HashMap<>(machine.getContext()));
                
                // Notify all registered listeners about the state change
            notifyStateChange(machine, model.getStartState().getId(), model.getStartState().getId(), 
            "INITIAL", MachineStateChangeEvent.EventType.INITIAL, null);

        return machine;
    }

    public Optional<MachineEntity> getMachine(Long machineId) {
        return machineRepository.findById(machineId);
    }
    
    public Optional<MachineEntity> getMachineByObject(String managedObjectId, String managedObjectType) {
        return machineRepository.findByManagedObjectIdAndManagedObjectType(managedObjectId, managedObjectType);
    }

    public List<MachineEntity> getAllMachines() {
        return machineRepository.findAll();
    }

    public List<MachineEntity> getMachinesByDefinition(String machineDefinitionId) {
        return machineRepository.findByMachineDefinitionId(machineDefinitionId);
    }

    @Transactional
    public MachineEntity executeTransition(Long machineId, String transitionId) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        final String definitionId = machine.getMachineDefinitionId();
        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Machine definition not found: " + definitionId));

        MachineDefinition model = machineDefinitionService.convertToModel(definition);

        // Find the transition
        Transition transition = model.getTransitions().stream()
                .filter(t -> t.getId().equals(transitionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        // Verify current state matches transition source
        if (!machine.getCurrentStateId().equals(transition.getSourceStateId())) {
            throw new IllegalStateException(
                    "Cannot execute transition " + transitionId +
                            ". Current state is " + machine.getCurrentStateId() +
                            " but transition requires " + transition.getSourceStateId());
        }

        boolean conditionMet = true;
        // Evaluate condition if present
        if (transition.getCondition() != null) {

            log.debug("expression: " + transition.getCondition().getExpression() + " context: " + machine.getContext());

            conditionMet = conditionEvaluator.evaluate(
                    transition.getCondition().getExpression(),
                    machine.getContext());
            if (!conditionMet) {
                log.debug("Transition condition not met: {} for trasitionID {}",  transition.getCondition().getExpression(), transitionId);
            }
        } else {
            log.debug("no condition, executing transition");
        }

        if (conditionMet) {
            // Record history before executing transition
            final String fromStateId = machine.getCurrentStateId();
            final Map<String, Object> contextSnapshot = new HashMap<>(machine.getContext());
            final String destinationStateId = transition.getDestinationStateId();

            // Execute transition
            machine.setCurrentStateId(destinationStateId);
            machine = machineRepository.save(machine);


            // Record history entry
            recordHistory(machine.getId(), fromStateId, destinationStateId,
                    transitionId, contextSnapshot);

            // Notify all registered listeners about the state change
            notifyStateChange(machine, fromStateId, destinationStateId, 
                    transitionId, MachineStateChangeEvent.EventType.STATE_CHANGED, null);
        }

        return machine;
    }

    @Transactional
    public MachineEntity updateContext(Long machineId, Map<String, Object> contextUpdates, String role) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        // Create a new HashMap to ensure Hibernate detects the change
        Map<String, Object> updatedContext = new HashMap<>();
        if (machine.getContext() != null) {
            updatedContext.putAll(machine.getContext());
        }
        
        // Merge the context updates
        updatedContext.putAll(contextUpdates);
        
        // Set the new context map to ensure Hibernate dirty checking works
        machine.setContext(updatedContext);

        // Record a history entry for this context update (even if state does not
        // change)
        // This lets the UI show how the context evolved over time.
        recordHistory(
                machine.getId(),
                machine.getCurrentStateId(),
                machine.getCurrentStateId(),
                "CONTEXT_UPDATE",
                new HashMap<>(machine.getContext()));

        // Save and flush to ensure context is persisted immediately
        machine = machineRepository.save(machine);
        entityManager.flush();
        entityManager.clear();

        MachineEntity fromDb = machineRepository.findById(machine.getId()).orElseThrow();
        log.debug("### after save : machine: {} context: {}", fromDb.getId(), fromDb.getContext());

        // Notify all registered listeners about the context update
        notifyStateChange(machine, machine.getCurrentStateId(), machine.getCurrentStateId(),
                null, MachineStateChangeEvent.EventType.CONTEXT_UPDATE, role);

        return machine;
        // // After updating context, check for available transitions and navigate
        // // automatically
        // List<Transition> availableTransitions = getAvailableTransitionsInternal(machine, role);

        // if (availableTransitions.isEmpty()) {
        //     // No transitions available, just return the updated machine
        //     return machine;
        // } else if (availableTransitions.size() == 1) {
        //     // Exactly one transition available, execute it automatically
        //     Transition transition = availableTransitions.get(0);
        //     log.info("Auto-executing transition {} after context update", transition.getId());
        //     return executeTransition(machineId, transition.getId());
        // } else {
        //     // Multiple transitions available, log WARN and let user select
        //     log.warn("Multiple transitions available ({} transitions) after context update for machine {}. " +
        //             "User must select which transition to execute. Available transitions: {}",
        //             availableTransitions.size(), machineId,
        //             availableTransitions.stream().map(Transition::getId).toList());
        //     return machine;
        // }
    }

    public List<Transition> getAvailableTransitions(@NonNull Long machineId) {
        return getAvailableTransitions(machineId, null);
    }

    public List<Transition> getAvailableTransitions(Long machineId, String role) {
        if (machineId == null) {
            throw new IllegalArgumentException("Machine ID cannot be null");
        }
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));
        return getAvailableTransitionsInternal(machine, role);
    }

    /**
     * Gets all transitions from the current state without filtering by role or
     * condition.
     * Useful for displaying all possible transitions grouped by role.
     */
    public List<Transition> getAllTransitionsFromCurrentState(Long machineId) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(machine.getMachineDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Machine definition not found: " + machine.getMachineDefinitionId()));

        MachineDefinition model = machineDefinitionService.convertToModel(definition);

        // Get all transitions from current state without filtering
        return model.getTransitions().stream()
                .filter(t -> t.getSourceStateId().equals(machine.getCurrentStateId()))
                .toList();
    }

    /**
     * Internal helper method to get available transitions for a machine entity.
     * This avoids redundant database fetches when we already have the machine
     * entity.
     * 
     * @param machine    The machine entity
     * @param roleFilter Optional role to filter by. If provided, only transitions
     *                   for this role are returned.
     */
    private List<Transition> getAvailableTransitionsInternal(MachineEntity machine, String roleFilter) {
        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(machine.getMachineDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Machine definition not found: " + machine.getMachineDefinitionId()));

        MachineDefinition model = machineDefinitionService.convertToModel(definition);

        // Get all transitions from current state
        List<Transition> transitionsFromCurrentState = model.getTransitions().stream()
                .filter(t -> t.getSourceStateId().equals(machine.getCurrentStateId()))
                .toList();

        log.debug("Found {} transitions from current state: {}", transitionsFromCurrentState.size(),
                machine.getCurrentStateId());
        if (roleFilter != null && !roleFilter.isEmpty()) {
            log.debug("Filtering by role: {}", roleFilter);
        }

        // Filter by role and conditions
        List<Transition> result = transitionsFromCurrentState.stream()
                .filter(t -> {
                    // If a role filter is specified, only include transitions for that role
                    if (roleFilter != null && !roleFilter.isEmpty()) {
                        String transitionRole = t.getRole() != null && !t.getRole().isEmpty() ? t.getRole() : "No Role";
                        if (!roleFilter.equals(transitionRole)) {
                            log.debug("Transition {} filtered out: role '{}' doesn't match filter '{}'",
                                    t.getId(), transitionRole, roleFilter);
                            return false; // Transition doesn't match the requested role
                        }
                        // When filtering by role, we still need to check if user has access to this
                        // role
                        // if (!hasRole(machine.getContext(), roleFilter)) {
                        // log.debug("Transition {} filtered out: user doesn't have role '{}'",
                        // t.getId(), roleFilter);
                        // return false; // User doesn't have the required role
                        // }
                    } else {
                        // No role filter - check if transition requires a specific role and user has it
                        // if (t.getRole() != null && !t.getRole().isEmpty()) {
                        // if (!hasRole(machine.getContext(), t.getRole())) {
                        // log.debug("Transition {} filtered out: user doesn't have role '{}'",
                        // t.getId(), t.getRole());
                        // return false; // User doesn't have the required role
                        // }
                        // }
                    }

                    // Then check condition if present
                    if (t.getCondition() == null) {
                        log.debug("Transition {} passed all checks", t.getId());
                        return true;
                    }
                    
                    // try {
                    //     boolean result1 = conditionEvaluator.evaluate(t.getCondition().getExpression(),
                    //             machine.getContext());
                    //     if (result1) {
                    //         log.debug("Transition {} passed condition check", t.getId());
                    //     } else {
                    //         log.debug("Transition {} filtered out: condition not met", t.getId());
                    //     }
                    //     return result1;
                    // } catch (Exception e) {
                    //     log.debug("Transition {} filtered out: condition evaluation error: {}", t.getId(),
                    //             e.getMessage());
                    //     return false;
                    // }
                    
                    // do not filter by condition
                    return true;
                })
                .toList();

        log.debug("Returning {} available transitions", result.size());
        return result;
    }

    /**
     * Checks if the context contains a user with the specified role.
     * Supports both "users" array format and direct "role" field.
     */
    private boolean hasRole(Map<String, Object> context, String requiredRole) {
        if (context == null || requiredRole == null) {
            return false;
        }

        // Check if there's a direct "role" field in context
        Object roleObj = context.get("role");
        if (roleObj != null && requiredRole.equals(String.valueOf(roleObj))) {
            return true;
        }

        // Check if there's a "users" array with users having the role
        Object usersObj = context.get("users");
        if (usersObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> users = (List<Object>) usersObj;
            for (Object userObj : users) {
                if (userObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> user = (Map<String, Object>) userObj;
                    Object userRole = user.get("role");
                    if (userRole != null && requiredRole.equals(String.valueOf(userRole))) {
                        return true;
                    }
                }
            }
        }

        // Check if there's a "currentUser" object with a role
        Object currentUserObj = context.get("currentUser");
        if (currentUserObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> currentUser = (Map<String, Object>) currentUserObj;
            Object userRole = currentUser.get("role");
            if (userRole != null && requiredRole.equals(String.valueOf(userRole))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Records a history entry for a state transition.
     */
    private void recordHistory(Long machineId, String fromStateId, String toStateId,
            String transitionId, Map<String, Object> contextSnapshot) {
        MachineHistoryEntity history = new MachineHistoryEntity();
        history.setMachineId(machineId);
        history.setFromStateId(fromStateId);
        history.setToStateId(toStateId);
        history.setTransitionId(transitionId);
        history.setContextSnapshot(new HashMap<>(contextSnapshot));
        machineHistoryRepository.save(history);
    }

    /**
     * Gets the history of state transitions for a machine instance.
     * 
     * @param machineId The machine instance ID
     * @return List of history entries ordered by timestamp (ascending - oldest
     *         first)
     */
    public List<MachineHistoryEntity> getMachineHistory(Long machineId) {
        return machineHistoryRepository.findByMachineIdOrderByTimestampAsc(machineId);
    }

    /**
     * Gets the history of state transitions for a machine instance in reverse
     * chronological order.
     * 
     * @param machineId The machine instance ID
     * @return List of history entries ordered by timestamp (descending - newest
     *         first)
     */
    public List<MachineHistoryEntity> getMachineHistoryDesc(Long machineId) {
        return machineHistoryRepository.findByMachineIdOrderByTimestampDesc(machineId);
    }

    /**
     * Notifies all registered listeners about a machine state change.
     * 
     * @param machine The machine entity
     * @param fromStateId The previous state ID (null for initial state)
     * @param toStateId The new state ID
     * @param transitionId The transition ID that caused the change (null for context updates)
     * @param eventType The type of event
     * @param role The role that triggered the change (if applicable)
     */
    private void notifyStateChange(MachineEntity machine, String fromStateId, String toStateId,
            String transitionId, MachineStateChangeEvent.EventType eventType, String role) {
        try {
            MachineStateChangeEvent event = MachineStateChangeEvent.builder()
                    .machineId(machine.getId())
                    .managedObjectId(machine.getManagedObjectId())
                    .managedObjectType(machine.getManagedObjectType())
                    .machineDefinitionId(machine.getMachineDefinitionId())
                    .fromStateId(fromStateId)
                    .toStateId(toStateId)
                    .transitionId(transitionId)
                    .eventType(eventType)
                    .machineContext(new HashMap<>(machine.getContext()))
                    .timestamp(LocalDateTime.now())
                    .role(role)
                    .build();

            listenerRegistry.notifyListeners(event);
        } catch (Exception e) {
            log.warn("Failed to notify listeners about machine state change: {}", e.getMessage(), e);
        }
    }

}
