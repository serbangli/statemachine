package com.statemachine.integration;

import com.statemachine.domain.entity.MachineDefinitionEntity;
import com.statemachine.domain.entity.MachineEntity;
import com.statemachine.domain.model.Transition;
import com.statemachine.repository.MachineDefinitionRepository;
import com.statemachine.repository.MachineRepository;
import com.statemachine.service.MachineDefinitionService;
import com.statemachine.service.MachineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StateMachineIntegrationTest {

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    @Autowired
    private MachineService machineService;

    @Autowired
    private MachineDefinitionRepository machineDefinitionRepository;

    @Autowired
    private MachineRepository machineRepository;

    private String machineDefinitionId = "first";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up before each test
        machineRepository.deleteAll();
        machineDefinitionRepository.deleteAll();

        // Load the machine definition from XML file
        machineDefinitionService.loadFromXml("first.xml");
    }

    @Test
    void testLoadMachineDefinitionFromXml() {
        // Verify machine definition was loaded
        MachineDefinitionEntity definition = machineDefinitionRepository.findById(machineDefinitionId)
            .orElseThrow();

        assertNotNull(definition);
        assertEquals("first", definition.getId());
        assertNotNull(definition.getStates());
        assertNotNull(definition.getTransitions());
        assertTrue(definition.getStates().size() >= 3); // start, one, two, end
        assertTrue(definition.getTransitions().size() >= 3); // t0, t1, t2
    }

    @Test
    void testCreateMachineInstance() {
        // Create initial context
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "john");

        // Create machine instance
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        assertNotNull(machine);
        assertNotNull(machine.getId());
        assertEquals(machineDefinitionId, machine.getMachineDefinitionId());
        assertEquals("start", machine.getCurrentStateId());
        assertEquals("john", machine.getContext().get("name"));
    }

    @Test
    void testExecuteTransitionWithoutCondition() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "john");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Execute transition t0 (start -> one) - no condition
        MachineEntity updatedMachine = machineService.executeTransition(machine.getId(), "t0");

        assertEquals("one", updatedMachine.getCurrentStateId());
    }

    @Test
    void testExecuteTransitionWithConditionMet() {
        // Create machine instance with context that meets condition
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "john");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Move to state "one" first
        machine = machineService.executeTransition(machine.getId(), "t0");
        assertEquals("one", machine.getCurrentStateId());

        // Execute transition t1 (one -> two) with condition name == 'john'
        MachineEntity updatedMachine = machineService.executeTransition(machine.getId(), "t1");

        assertEquals("two", updatedMachine.getCurrentStateId());
    }

    @Test
    void testExecuteTransitionWithConditionNotMet() {
        // Create machine instance with context that doesn't meet condition
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "jane"); // Not "john"
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Move to state "one" first
        MachineEntity machineInStateOne = machineService.executeTransition(machine.getId(), "t0");
        assertEquals("one", machineInStateOne.getCurrentStateId());

        // Try to execute transition t1 (one -> two) with condition name == 'john'
        // Should fail because condition is not met
        final Long machineId = machineInStateOne.getId();
        assertThrows(IllegalStateException.class, () -> {
            machineService.executeTransition(machineId, "t1");
        });
    }

    @Test
    void testGetAvailableTransitions() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "john");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Get available transitions from start state
        List<Transition> availableTransitions = machineService.getAvailableTransitions(machine.getId());

        assertNotNull(availableTransitions);
        assertEquals(1, availableTransitions.size());
        assertEquals("t0", availableTransitions.get(0).getId());
    }

    @Test
    void testGetAvailableTransitionsWithCondition() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "john");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Move to state "one"
        MachineEntity machineInStateOne = machineService.executeTransition(machine.getId(), "t0");

        // Get available transitions from state "one"
        List<Transition> availableTransitions = machineService.getAvailableTransitions(machineInStateOne.getId());

        assertNotNull(availableTransitions);
        // Should only return t1 if condition is met
        assertEquals(1, availableTransitions.size());
        assertEquals("t1", availableTransitions.get(0).getId());
    }

    @Test
    void testGetAvailableTransitionsWithConditionNotMet() {
        // Create machine instance with context that doesn't meet condition
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "jane");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Move to state "one"
        MachineEntity machineInStateOne = machineService.executeTransition(machine.getId(), "t0");

        // Get available transitions from state "one"
        List<Transition> availableTransitions = machineService.getAvailableTransitions(machineInStateOne.getId());

        // Should return empty list because condition is not met
        assertTrue(availableTransitions.isEmpty());
    }

    @Test
    void testUpdateContext() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "jane");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Update context
        Map<String, Object> contextUpdates = new HashMap<>();
        contextUpdates.put("name", "john");
        contextUpdates.put("age", "30");

        MachineEntity updatedMachine = machineService.updateContext(machine.getId(), contextUpdates, null);

        assertEquals("john", updatedMachine.getContext().get("name"));
        assertEquals("30", updatedMachine.getContext().get("age"));
    }

    @Test
    void testCompleteStateMachineFlow() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "john");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Verify initial state
        assertEquals("start", machine.getCurrentStateId());

        // Execute transition t0: start -> one
        machine = machineService.executeTransition(machine.getId(), "t0");
        assertEquals("one", machine.getCurrentStateId());

        // Execute transition t1: one -> two (with condition)
        machine = machineService.executeTransition(machine.getId(), "t1");
        assertEquals("two", machine.getCurrentStateId());

        // Execute transition t2: two -> end
        machine = machineService.executeTransition(machine.getId(), "t2");
        assertEquals("end", machine.getCurrentStateId());
    }

    @Test
    void testExecuteTransitionFromWrongState() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("name", "john");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext);

        // Try to execute transition t1 from start state (should fail)
        assertThrows(IllegalStateException.class, () -> {
            machineService.executeTransition(machine.getId(), "t1");
        });
    }
}

