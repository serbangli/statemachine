package com.statemachine.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.statemachine.dto.task.TaskReviewRequest;
import com.statemachine.dto.task.TaskReviewerRequest;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    private String machineDefinitionId = "taskReview";
    private String taskId = "test-task-001";
    private String ownerId = "owner@example.com";

    @BeforeEach
    void setUp() throws Exception {
        // Load machine definition
        machineDefinitionService.loadFromXml("taskReview.xml");
    }

    @Test
    void testCreateTask() throws Exception {
        TaskReviewRequest request = new TaskReviewRequest();
        request.setTaskId(taskId);
        request.setOwnerId(ownerId);
        request.setMachineDefinitionId(machineDefinitionId);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineDefinitionId").value(machineDefinitionId))
                .andExpect(jsonPath("$.currentStateId").value("IN_REVIEW"))
                .andExpect(jsonPath("$.context.ownerId").value(ownerId));
    }

    @Test
    void testCreateTaskWithInvalidDefinition() throws Exception {
        TaskReviewRequest request = new TaskReviewRequest();
        request.setTaskId(taskId);
        request.setOwnerId(ownerId);
        request.setMachineDefinitionId("non-existent-definition");

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddReviewer() throws Exception {
        // First create a task
        createTask();

        TaskReviewerRequest reviewerRequest = new TaskReviewerRequest();
        reviewerRequest.setTaskId(taskId);
        reviewerRequest.setReviewerId("reviewer-001");
        reviewerRequest.setReviewerName("John Doe");
        reviewerRequest.setReviewerEmail("john.doe@example.com");

        mockMvc.perform(post("/api/tasks/" + taskId + "/reviewer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists())
                .andExpect(jsonPath("$.context.reqreviewers").exists());
    }

    @Test
    void testAddReviewerToNonExistentTask() throws Exception {
        TaskReviewerRequest reviewerRequest = new TaskReviewerRequest();
        reviewerRequest.setTaskId("non-existent-task");
        reviewerRequest.setReviewerId("reviewer-001");
        reviewerRequest.setReviewerName("John Doe");
        reviewerRequest.setReviewerEmail("john.doe@example.com");

        mockMvc.perform(post("/api/tasks/non-existent-task/reviewer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewerRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubmitReview() throws Exception {
        // First create a task
        createTask();

        TaskReviewerRequest reviewRequest = new TaskReviewerRequest();
        reviewRequest.setTaskId(taskId);
        reviewRequest.setReviewerId("reviewer-001");
        reviewRequest.setReviewerName("John Doe");
        reviewRequest.setReviewerEmail("john.doe@example.com");

        mockMvc.perform(post("/api/tasks/" + taskId + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists())
                .andExpect(jsonPath("$.context.reviewers").exists());
    }

    @Test
    void testSubmitReviewToNonExistentTask() throws Exception {
        TaskReviewerRequest reviewRequest = new TaskReviewerRequest();
        reviewRequest.setTaskId("non-existent-task");
        reviewRequest.setReviewerId("reviewer-001");
        reviewRequest.setReviewerName("John Doe");
        reviewRequest.setReviewerEmail("john.doe@example.com");

        mockMvc.perform(post("/api/tasks/non-existent-task/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCloseReview() throws Exception {
        // First create a task
        createTask();

        mockMvc.perform(post("/api/tasks/" + taskId + "/review/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());
    }

    @Test
    void testCloseReviewNonExistentTask() throws Exception {
        mockMvc.perform(post("/api/tasks/non-existent-task/review/close"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testReopenReview() throws Exception {
        // First create a task
        createTask();
        // Close it first
        closeReview();

        mockMvc.perform(post("/api/tasks/" + taskId + "/review/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());
    }

    @Test
    void testRollbackReview() throws Exception {
        // First create a task
        createTask();
        mockMvc.perform(post("/api/tasks/" + taskId + "/review/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());

        mockMvc.perform(post("/api/tasks/" + taskId + "/review/rollback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());
    }

    @Test
    void testStartIntegration() throws Exception {
        // First create a task
        createTask();
        mockMvc.perform(post("/api/tasks/" + taskId + "/review/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());

        mockMvc.perform(post("/api/tasks/" + taskId + "/integration/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());
    }

    @Test
    void testCancelIntegration() throws Exception {
        // First create a task
        createTask();
        mockMvc.perform(post("/api/tasks/" + taskId + "/review/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());

        mockMvc.perform(post("/api/tasks/" + taskId + "/integration/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());

        mockMvc.perform(post("/api/tasks/" + taskId + "/integration/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());
    }

    @Test
    void testFinishIntegration() throws Exception {
        // First create a task
        createTask();
        mockMvc.perform(post("/api/tasks/" + taskId + "/review/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());

        mockMvc.perform(post("/api/tasks/" + taskId + "/integration/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());

        mockMvc.perform(post("/api/tasks/" + taskId + "/integration/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists());
    }

    @Test
    void testGetAvailableTransitions() throws Exception {
        // First create a task
        createTask();

        mockMvc.perform(get("/api/tasks/" + taskId + "/transitions/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetAvailableTransitionsWithRoleFilter() throws Exception {
        // First create a task
        createTask();

        mockMvc.perform(get("/api/tasks/" + taskId + "/transitions/available")
                .param("role", "owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetAvailableTransitionsForNonExistentTask() throws Exception {
        mockMvc.perform(get("/api/tasks/non-existent-task/transitions/available"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCompleteWorkflow() throws Exception {
        // Create task
        String response = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTaskRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").value("IN_REVIEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> taskResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
        });
        assertNotNull(taskResponse.get("machineId"));

        // Add reviewer
        TaskReviewerRequest reviewerRequest = new TaskReviewerRequest();
        reviewerRequest.setTaskId(taskId);
        reviewerRequest.setReviewerId("reviewer-001");
        reviewerRequest.setReviewerName("John Doe");
        reviewerRequest.setReviewerEmail("john.doe@example.com");

        response = mockMvc.perform(post("/api/tasks/" + taskId + "/reviewer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStateId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        taskResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
        });
        String stateAfterReviewer = (String) taskResponse.get("currentStateId");
        assertNotNull(stateAfterReviewer);

        // Get available transitions
        mockMvc.perform(get("/api/tasks/" + taskId + "/transitions/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testAddMultipleReviewers() throws Exception {
        // Create task
        createTask();

        // Add first reviewer
        TaskReviewerRequest reviewerRequest1 = new TaskReviewerRequest();
        reviewerRequest1.setTaskId(taskId);
        reviewerRequest1.setReviewerId("reviewer-001");
        reviewerRequest1.setReviewerName("John Doe");
        reviewerRequest1.setReviewerEmail("john.doe@example.com");

        mockMvc.perform(post("/api/tasks/" + taskId + "/reviewer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewerRequest1)))
                .andExpect(status().isOk());

        // Add second reviewer
        TaskReviewerRequest reviewerRequest2 = new TaskReviewerRequest();
        reviewerRequest2.setTaskId(taskId);
        reviewerRequest2.setReviewerId("reviewer-002");
        reviewerRequest2.setReviewerName("Jane Smith");
        reviewerRequest2.setReviewerEmail("jane.smith@example.com");

        String response = mockMvc.perform(post("/api/tasks/" + taskId + "/reviewer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewerRequest2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> taskResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reqreviewers = (List<Map<String, Object>>) ((Map<String, Object>) taskResponse
                .get("context")).get("reqreviewers");

        assertNotNull(reqreviewers);
        assertTrue(reqreviewers.size() >= 2);
    }

    // Helper method to create a task
    private void createTask() throws Exception {
        TaskReviewRequest request = createTaskRequest();
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void closeReview() throws Exception {
        mockMvc.perform(post("/api/tasks/" + taskId + "/review/close"))
                .andExpect(status().isOk());
    }

    private TaskReviewRequest createTaskRequest() {
        TaskReviewRequest request = new TaskReviewRequest();
        request.setTaskId(taskId);
        request.setOwnerId(ownerId);
        request.setMachineDefinitionId(machineDefinitionId);
        return request;
    }
}
