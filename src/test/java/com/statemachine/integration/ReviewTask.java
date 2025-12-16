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
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewTask {

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    @Autowired
    private MachineService machineService;

    @Autowired
    private MachineDefinitionRepository machineDefinitionRepository;

    @Autowired
    private MachineRepository machineRepository;

    private String machineDefinitionId = "review-task";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up before each test
        machineRepository.deleteAll();
        machineDefinitionRepository.deleteAll();

        // Load the machine definition from XML file
        machineDefinitionService.loadFromXml("review-task.xml");
    }

    @Test
    void testLoadMachineDefinitionFromXml() {
        // Verify machine definition was loaded
        MachineDefinitionEntity definition = machineDefinitionRepository.findById(machineDefinitionId)
            .orElseThrow();

        assertNotNull(definition);
        assertEquals("review-task", definition.getId());
        assertNotNull(definition.getStates());
        assertNotNull(definition.getTransitions());
        assertTrue(definition.getStates().size() >= 6); //
        assertTrue(definition.getTransitions().size() == 7); //
    }

    @Test
    void testCreateMachineInstance() {
        // Create initial context
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("owner", "john");

        // Create machine instance
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext, null, null);

        assertNotNull(machine);
        assertNotNull(machine.getId());
        assertEquals(machineDefinitionId, machine.getMachineDefinitionId());
        assertEquals("start", machine.getCurrentStateId());
        assertEquals("john", machine.getContext().get("owner"));        
    }

    @Test
    void testExecuteTransitionWithoutCondition() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext, null, null);

        // Execute transition t0 (start -> one) - no condition
        MachineEntity updatedMachine = machineService.executeTransition(machine.getId(), "o_t0");

        assertEquals("IN_REVIEW", updatedMachine.getCurrentStateId());
        updatedMachine = machineService.executeTransition(machine.getId(), "o_t1");

        assertEquals("REVIEWED", updatedMachine.getCurrentStateId());
    }

    @Test
    void testExecuteTransitionFlow() {
        // Create machine instance with context that meets condition
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("owner", "johnathan");
        List<Map<String, Object>> reqreviewers = new ArrayList<>();
        reqreviewers.add(new HashMap<String, Object>() {{ put("name", "john"); }});
        reqreviewers.add(new HashMap<String, Object>() {{ put("name", "jane"); }});
        reqreviewers.add(new HashMap<String, Object>() {{ put("name", "doe"); }});
        initialContext.put("reqreviewers", reqreviewers);
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext, null, null);

        // Move to state "one" first
        MachineEntity updatedMachine = machineService.executeTransition(machine.getId(), "o_t0");
        assertEquals("IN_REVIEW", updatedMachine.getCurrentStateId());
        
        Map<String, Object> reviewersContext = new HashMap<>();
        List<Map<String, Object>> reviewers = new ArrayList<>();
        reviewers.add(new HashMap<String, Object>() {{ put("name", "john"); }});        
        reviewersContext.put("reviewers", reviewers);
        
        updatedMachine = machineService.updateContext(machine.getId(), reviewersContext, "reviewer");
        assertEquals("IN_REVIEW", updatedMachine.getCurrentStateId());

        
        reviewersContext = new HashMap<>();
        reviewers = new ArrayList<>();
        reviewers.add(new HashMap<String, Object>() {{ put("name", "john"); }});       
        reviewers.add(new HashMap<String, Object>() {{ put("name", "jane"); }});
        reviewers.add(new HashMap<String, Object>() {{ put("name", "doe"); }}); 
        reviewersContext.put("reviewers", reviewers);

        updatedMachine = machineService.updateContext(machine.getId(), reviewersContext, "reviewer");
        assertEquals("REVIEWED", updatedMachine.getCurrentStateId());

    }

   
    @Test
    void testGetAvailableTransitions() {
        // Create machine instance
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("owner", "john");
        MachineEntity machine = machineService.createMachineInstance(machineDefinitionId, initialContext, null, null);

        assertEquals("start", machine.getCurrentStateId());
        assertEquals("john", machine.getContext().get("owner"));
    

        // Get available transitions from start state
        List<Transition> availableTransitions = machineService.getAvailableTransitions(machine.getId(), "owner");

        assertNotNull(availableTransitions);
        assertEquals(1, availableTransitions.size());
        assertEquals("o_t0", availableTransitions.get(0).getId());
        

        // Get available transitions from start state
        availableTransitions = machineService.getAvailableTransitions(machine.getId(), "reviewer");

        assertNotNull(availableTransitions);
        assertEquals(0, availableTransitions.size());        
    }

  
}

