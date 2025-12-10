package com.statemachine.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.statemachine.dto.CreateMachineRequest;
import com.statemachine.dto.TransitionRequest;
import com.statemachine.dto.UpdateContextRequest;
import com.statemachine.service.MachineDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MachineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    private String machineDefinitionId = "first";

    @BeforeEach
    void setUp() throws Exception {
        // Load machine definition
        machineDefinitionService.loadFromXml("first.xml");
    }

    @Test
    void testCreateMachine() throws Exception {
        CreateMachineRequest request = new CreateMachineRequest();
        request.setMachineDefinitionId(machineDefinitionId);
        Map<String, Object> context = new HashMap<>();
        context.put("name", "john");
        request.setInitialContext(context);

        mockMvc.perform(post("/api/machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineDefinitionId").value(machineDefinitionId))
                .andExpect(jsonPath("$.currentStateId").value("start"))
                .andExpect(jsonPath("$.context.name").value("john"));
    }

    @Test
    void testGetMachine() throws Exception {
        // First create a machine
        CreateMachineRequest createRequest = new CreateMachineRequest();
        createRequest.setMachineDefinitionId(machineDefinitionId);
        createRequest.setInitialContext(new HashMap<>());

        String response = mockMvc.perform(post("/api/machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract machine ID from response
        Map<String, Object> machineResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        Long machineId = Long.valueOf(machineResponse.get("id").toString());

        // Then get the machine
        mockMvc.perform(get("/api/machines/" + machineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(machineId))
                .andExpect(jsonPath("$.machineDefinitionId").value(machineDefinitionId));
    }

    @Test
    void testExecuteTransition() throws Exception {
        // Create a machine
        CreateMachineRequest createRequest = new CreateMachineRequest();
        createRequest.setMachineDefinitionId(machineDefinitionId);
        Map<String, Object> context = new HashMap<>();
        context.put("name", "john");
        createRequest.setInitialContext(context);

        String response = mockMvc.perform(post("/api/machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> machineResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        Long machineId = Long.valueOf(machineResponse.get("id").toString());

        // Execute transition
        TransitionRequest transitionRequest = new TransitionRequest();
        transitionRequest.setTransitionId("t0");

        mockMvc.perform(post("/api/machines/" + machineId + "/transitions/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").value("one"));
    }

    @Test
    void testGetAvailableTransitions() throws Exception {
        // Create a machine
        CreateMachineRequest createRequest = new CreateMachineRequest();
        createRequest.setMachineDefinitionId(machineDefinitionId);
        Map<String, Object> context = new HashMap<>();
        context.put("name", "john");
        createRequest.setInitialContext(context);

        String response = mockMvc.perform(post("/api/machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> machineResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        Long machineId = Long.valueOf(machineResponse.get("id").toString());

        // Get available transitions
        mockMvc.perform(get("/api/machines/" + machineId + "/transitions/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t0"));
    }

    @Test
    void testUpdateContext() throws Exception {
        // Create a machine
        CreateMachineRequest createRequest = new CreateMachineRequest();
        createRequest.setMachineDefinitionId(machineDefinitionId);
        createRequest.setInitialContext(new HashMap<>());

        String response = mockMvc.perform(post("/api/machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> machineResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        Long machineId = Long.valueOf(machineResponse.get("id").toString());

        // Update context
        UpdateContextRequest updateRequest = new UpdateContextRequest();
        Map<String, Object> context = new HashMap<>();
        context.put("name", "john");
        context.put("age", "30");
        updateRequest.setContext(context);

        mockMvc.perform(put("/api/machines/" + machineId + "/context")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.name").value("john"))
                .andExpect(jsonPath("$.context.age").value("30"));
    }
}

