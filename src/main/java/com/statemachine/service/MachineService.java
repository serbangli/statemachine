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
            boolean conditionMet = conditionEvaluator.evaluate(
                transition.getCondition().getExpression(),
                machine.getContext()
            );
            if (!conditionMet) {
                throw new IllegalStateException(
                    "Transition condition not met: " + transition.getCondition().getExpression()
                );
            }
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
        
        return machine;
    }

    @Transactional
    public MachineEntity updateContext(Long machineId, Map<String, Object> contextUpdates) {
        MachineEntity machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));

        if (machine.getContext() == null) {
            machine.setContext(new HashMap<>());
        }

        // Update context directly - JSON storage handles serialization automatically
        machine.getContext().putAll(contextUpdates);
        machine = machineRepository.save(machine);

        // After updating context, check for available transitions and navigate automatically
        List<Transition> availableTransitions = getAvailableTransitionsInternal(machine);
        
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
        MachineEntity machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new IllegalArgumentException("Machine not found: " + machineId));
        return getAvailableTransitionsInternal(machine);
    }

    /**
     * Internal helper method to get available transitions for a machine entity.
     * This avoids redundant database fetches when we already have the machine entity.
     */
    private List<Transition> getAvailableTransitionsInternal(MachineEntity machine) {
        MachineDefinitionEntity definition = machineDefinitionService.getDefinition(machine.getMachineDefinitionId())
            .orElseThrow(() -> new IllegalArgumentException("Machine definition not found: " + machine.getMachineDefinitionId()));

        MachineDefinition model = machineDefinitionService.convertToModel(definition);

        // Get all transitions from current state
        List<Transition> availableTransitions = model.getTransitions().stream()
            .filter(t -> t.getSourceStateId().equals(machine.getCurrentStateId()))
            .toList();

        // Filter by conditions
        return availableTransitions.stream()
            .filter(t -> {
                if (t.getCondition() == null) {
                    return true;
                }
                try {
                    boolean result = conditionEvaluator.evaluate(t.getCondition().getExpression(), machine.getContext());
                    return result;
                } catch (Exception e) {
                    return false;
                }
            })
            .toList();
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

