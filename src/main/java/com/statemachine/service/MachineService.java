package com.statemachine.service;

import com.statemachine.domain.entity.MachineEntity;
import com.statemachine.domain.entity.MachineDefinitionEntity;
import com.statemachine.domain.entity.MachineHistoryEntity;
import com.statemachine.domain.model.MachineDefinition;
import com.statemachine.domain.model.Transition;
import com.statemachine.repository.MachineRepository;
import com.statemachine.repository.MachineHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.statemachine.websocket.MachineStateWebSocketHandler;
import com.statemachine.websocket.MachineStateWebSocketHandler.MachineStateChangeMessage;
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
    private MachineStateWebSocketHandler machineStateWebSocketHandler;

    @Transactional
    public MachineEntity createMachineInstance(String machineDefinitionId, Map<String, Object> initialContext) {
        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(machineDefinitionId)
            .orElseThrow(() -> new IllegalArgumentException("Machine definition not found: " + machineDefinitionId));

        MachineDefinition model = machineDefinitionService.convertToModel(definition);
        
        if (model.getStartState() == null) {
            throw new IllegalStateException("Machine definition must have a start state");
        }

        MachineEntity machine = new MachineEntity();
        machine.setMachineDefinitionId(machineDefinitionId);
        machine.setCurrentStateId(model.getStartState().getId());
        machine.setContext(initialContext != null ? new HashMap<>(initialContext) : new HashMap<>());

        machine = machineRepository.save(machine);
        
        // Record initial state in history
        recordHistory(machine.getId(), null, model.getStartState().getId(), 
                     "INITIAL", new HashMap<>(machine.getContext()));
        
        return machine;
    }

    public Optional<MachineEntity> getMachine(Long machineId) {
        return machineRepository.findById(machineId);
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
                " but transition requires " + transition.getSourceStateId()
            );
        }

        // Evaluate condition if present
        if (transition.getCondition() != null) {
            
            log.debug("expression: "+transition.getCondition().getExpression()+ " context: "+machine.getContext());

            boolean conditionMet = conditionEvaluator.evaluate(
                transition.getCondition().getExpression(),
                machine.getContext()
            );
            if (!conditionMet) {
                throw new IllegalStateException(
                    "Transition condition not met: " + transition.getCondition().getExpression()
                );
            }
        }else{
            log.debug("no condition, executing transition");
        }

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

        // Notify WebSocket listeners about the state change
        try {
            MachineStateChangeMessage msg = new MachineStateChangeMessage(
                machine.getId(),
                fromStateId,
                destinationStateId,
                transitionId,
                "STATE_CHANGED",
                machine.getContext()
            );
            machineStateWebSocketHandler.broadcastStateChange(msg);
        } catch (Exception e) {
            log.warn("Failed to broadcast machine state change over WebSocket", e);
        }
        
        return machine;
    }

    @Transactional
    public MachineEntity updateContext(Long machineId, Map<String, Object> contextUpdates, String role) {
        MachineEntity machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        if (machine.getContext() == null) {
            machine.setContext(new HashMap<>());
        }

        // Update context directly - JSON storage handles serialization automatically
        machine.getContext().putAll(contextUpdates);
        machine = machineRepository.save(machine);

        // Record a history entry for this context update (even if state does not change)
        // This lets the UI show how the context evolved over time.
        recordHistory(
            machine.getId(),
            machine.getCurrentStateId(),
            machine.getCurrentStateId(),
            "CONTEXT_UPDATE",
            new HashMap<>(machine.getContext())
        );

        
        // Notify WebSocket listeners about the state change
        try {
            MachineStateChangeMessage msg = new MachineStateChangeMessage(
                machine.getId(),
                machine.getCurrentStateId(),
                machine.getCurrentStateId(),
                null,
                "CONTEXT_UPDATE",
                machine.getContext()
            );
            machineStateWebSocketHandler.broadcastStateChange(msg);
        } catch (Exception e) {
            log.warn("Failed to broadcast machine state change over WebSocket", e);
        }
        // After updating context, check for available transitions and navigate automatically
        List<Transition> availableTransitions = getAvailableTransitionsInternal(machine, role);
        

        if (availableTransitions.isEmpty()) {
            // No transitions available, just return the updated machine
            return machine;
        } else if (availableTransitions.size() == 1) {
            // Exactly one transition available, execute it automatically
            Transition transition = availableTransitions.get(0);
            log.info("Auto-executing transition {} after context update", transition.getId());
            return executeTransition(machineId, transition.getId());
        } else {
            // Multiple transitions available, log WARN and let user select
            log.warn("Multiple transitions available ({} transitions) after context update for machine {}. " +
                    "User must select which transition to execute. Available transitions: {}",
                    availableTransitions.size(), machineId, 
                    availableTransitions.stream().map(Transition::getId).toList());
            return machine;
        }
    }

    public List<Transition> getAvailableTransitions(Long machineId) {
        return getAvailableTransitions(machineId, null);
    }

    public List<Transition> getAvailableTransitions(Long machineId, String role) {
        MachineEntity machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));
        return getAvailableTransitionsInternal(machine, role);
    }

    /**
     * Gets all transitions from the current state without filtering by role or condition.
     * Useful for displaying all possible transitions grouped by role.
     */
    public List<Transition> getAllTransitionsFromCurrentState(Long machineId) {
        MachineEntity machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));
        
        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(machine.getMachineDefinitionId())
            .orElseThrow(() -> new IllegalArgumentException("Machine definition not found: " + machine.getMachineDefinitionId()));

        MachineDefinition model = machineDefinitionService.convertToModel(definition);

        // Get all transitions from current state without filtering
        return model.getTransitions().stream()
            .filter(t -> t.getSourceStateId().equals(machine.getCurrentStateId()))
            .toList();
    }

    /**
     * Internal helper method to get available transitions for a machine entity.
     * This avoids redundant database fetches when we already have the machine entity.
     * 
     * @param machine The machine entity
     * @param roleFilter Optional role to filter by. If provided, only transitions for this role are returned.
     */
    private List<Transition> getAvailableTransitionsInternal(MachineEntity machine, String roleFilter) {
        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(machine.getMachineDefinitionId())
            .orElseThrow(() -> new IllegalArgumentException("Machine definition not found: " + machine.getMachineDefinitionId()));

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
                    // When filtering by role, we still need to check if user has access to this role
                    // if (!hasRole(machine.getContext(), roleFilter)) {
                    //     log.debug("Transition {} filtered out: user doesn't have role '{}'", t.getId(), roleFilter);
                    //     return false; // User doesn't have the required role
                    // }
                } else {
                    // No role filter - check if transition requires a specific role and user has it
                    // if (t.getRole() != null && !t.getRole().isEmpty()) {
                    //     if (!hasRole(machine.getContext(), t.getRole())) {
                    //         log.debug("Transition {} filtered out: user doesn't have role '{}'", t.getId(), t.getRole());
                    //         return false; // User doesn't have the required role
                    //     }
                    // }
                }
                
                // Then check condition if present
                if (t.getCondition() == null) {
                    log.debug("Transition {} passed all checks", t.getId());
                    return true;
                }
                try {
                    boolean result1 = conditionEvaluator.evaluate(t.getCondition().getExpression(), machine.getContext());
                    if (result1) {
                        log.debug("Transition {} passed condition check", t.getId());
                    } else {
                        log.debug("Transition {} filtered out: condition not met", t.getId());
                    }
                    return result1;
                } catch (Exception e) {
                    log.debug("Transition {} filtered out: condition evaluation error: {}", t.getId(), e.getMessage());
                    return false;
                }
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
     * @param machineId The machine instance ID
     * @return List of history entries ordered by timestamp (ascending - oldest first)
     */
    public List<MachineHistoryEntity> getMachineHistory(Long machineId) {
        return machineHistoryRepository.findByMachineIdOrderByTimestampAsc(machineId);
    }

    /**
     * Gets the history of state transitions for a machine instance in reverse chronological order.
     * @param machineId The machine instance ID
     * @return List of history entries ordered by timestamp (descending - newest first)
     */
    public List<MachineHistoryEntity> getMachineHistoryDesc(Long machineId) {
        return machineHistoryRepository.findByMachineIdOrderByTimestampDesc(machineId);
    }

}

