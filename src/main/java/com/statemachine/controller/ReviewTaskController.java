package com.statemachine.controller;

import com.statemachine.domain.model.Transition;
import com.statemachine.dto.task.TaskReviewRequest;
import com.statemachine.dto.task.TaskUserRequest;
import com.statemachine.dto.task.TaskReviewResponse;
import com.statemachine.domain.entity.MachineEntity;
import com.statemachine.service.MachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "ReviewTasks", description = "API for managing review tasks")
@Slf4j
public class ReviewTaskController {

	private static final String TRUE_STR = "true";
	@Autowired
	private MachineService machineService;

	@Operation(summary = "Create a new task review machine instance", description = "Creates a new task review machine instance from a machine definition with an initial context")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Task review machine instance created successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping
	public ResponseEntity<TaskReviewResponse> createTask(@RequestBody TaskReviewRequest request) {
		try {
			Map<String, Object> initialContext = new HashMap<>();
			initialContext.put("ownerId", request.getOwnerId());

			MachineEntity machine = machineService.createMachineInstance(request.getMachineDefinitionId(),
					initialContext, request.getTaskId(), MachineEntity.ManagedObjectType.REVIEW_TASK.toString());
			return ResponseEntity.ok(convertToResponse(machine));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@Operation(summary = "Add a new reviewer to a task", description = "Adds a new reviewer to a task")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reviewer added successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/reviewer")
	public ResponseEntity<TaskReviewResponse> addReviewer(@RequestBody TaskUserRequest request) {
		try {
			MachineEntity machine = machineService
					.getMachineByObject(request.getTaskId(), MachineEntity.ManagedObjectType.REVIEW_TASK.toString())
					.orElseThrow(() -> new IllegalArgumentException("Task not found"));

			// // Prepare the new reviewer entry
			Map<String, Object> newReviewer = new HashMap<>();
			newReviewer.put("name", request.getUserName());
			newReviewer.put("email", request.getUserEmail());
			newReviewer.put("id", request.getUserId());

			Map<String, Object> initialContext = getNewMap(machine.getContext());

			addToContext(initialContext, newReviewer, "reqreviewers");

			machine = machineService.updateContext(machine.getId(), initialContext, "owner");
			machine = machineService.executeTransition(machine.getId(), "o_t0");
			return ResponseEntity.ok(convertToResponse(machine));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}

	}

	private void addToContext(Map<String, Object> map, Map<String, Object> item, String key) {
		List<Map<String, Object>> reviewers = new ArrayList<>();
		// Update the context with the new reviewer
		if (map.containsKey(key)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> existingReviewers = (List<Map<String, Object>>) map.get(key);
			if (!itemMapExists(existingReviewers, item)) {
				reviewers.add(item);
			}
			if (existingReviewers != null) {
				for (Map<String, Object> reviewer : existingReviewers) {
					reviewers.add(reviewer);
				}
			}
		} else {
			reviewers.add(item);
		}
		map.put(key, reviewers);
	}

	private boolean isInContext(Map<String, Object> ctxMap, Map<String, Object> item, String ctxKey) {
		boolean toReturn = false;

		// Update the context with the new reviewer
		if (null!=ctxKey && ctxKey.trim().length()>0 && ctxMap.containsKey(ctxKey)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> existingReviewers = (List<Map<String, Object>>) ctxMap.get(ctxKey);
			toReturn = itemMapExists(existingReviewers, item);
		}

		return toReturn;
	}

	private boolean itemMapExists(List<Map<String, Object>> list, Map<String, Object> item) {
		boolean toReturn = false;
		for (Map<String, Object> current : list) {
			boolean isTheSame = true;
			for (String mapKey : current.keySet()) {
				if (!item.containsKey(mapKey) || !item.get(mapKey).equals(current.get(mapKey))) {
					isTheSame = false;
				}
			}
			if (isTheSame) {
				toReturn = isTheSame;
				break;
			}
		}
		return toReturn;
	}

	private Map<String, Object> getNewMap(Map<String, Object> map) {
		Map<String, Object> context = new HashMap<>();
		if (map != null) {
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				context.put(entry.getKey(), entry.getValue());
			}
		}
		return context;
	}

	@Operation(summary = "Owner closes review task", description = "Owner closes review task")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Review task closed successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/review/close")
	public ResponseEntity<TaskReviewResponse> closeReview(@PathVariable String taskId) {
		return executeTask(taskId, "o_t1", "owner", null);
	}

	@Operation(summary = "Owner reopens review task", description = "Owner reopens review task")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Review task reopened successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/review/reopen")
	public ResponseEntity<TaskReviewResponse> reopenReview(@PathVariable String taskId) {
		return executeTask(taskId, "o_t2", "owner", null);
	}

	@Operation(summary = "Owner rolls back review task", description = "Owner rolls back review task")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Review task rolled back successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/review/rollback")
	public ResponseEntity<TaskReviewResponse> rollbackReview(@PathVariable String taskId) {
		Map<String, Object> context = new HashMap<>();
		List<Map<String, Object>> reviewers = new ArrayList<>();
		context.put("reviewers", reviewers);
		return executeTask(taskId, "o_t3", "owner", context);
	}

	@Operation(summary = "Owner start integrating review task ", description = "Owner start integrating review task into content")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Review task start integrating successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/integration/start")
	public ResponseEntity<TaskReviewResponse> startIntegration(@PathVariable String taskId) {

		return executeTask(taskId, "o_t4", "owner", null);
	}

	@Operation(summary = "Owner cancel integrating review task ", description = "Owner cancel integrating review task into content")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Review task cancel integrating successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/integration/cancel")
	public ResponseEntity<TaskReviewResponse> cancelIntegration(@PathVariable String taskId) {

		return executeTask(taskId, "o_t5", "owner", null);
	}

	@Operation(summary = "Owner publish review task ", description = "Owner publish review task into content")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Review task publish successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/integration/finish")
	public ResponseEntity<TaskReviewResponse> finishIntegration(@PathVariable String taskId) {

		return executeTask(taskId, "o_t6", "owner", null);
	}

	@Operation(summary = "Get available transitions", description = "Retrieves all available transitions from the current state. "
			+ "Optionally filters by role. Only returns transitions that meet role requirements and conditions.")
	@ApiResponse(responseCode = "200", description = "List of available transitions")
	@GetMapping("/{taskId}/transitions/available")
	public ResponseEntity<List<Transition>> getAvailableTransitions(
			@Parameter(description = "task ID", required = true) @PathVariable String taskId,
			@Parameter(description = "Optional role filter (e.g., 'owner', 'reviewer')") @RequestParam(required = false) String role) {
		try {
			MachineEntity machine = machineService
					.getMachineByObject(taskId, MachineEntity.ManagedObjectType.REVIEW_TASK.toString())
					.orElseThrow(() -> new IllegalArgumentException("Task not found"));

			List<Transition> transitions = machineService.getAvailableTransitions(machine.getId(), role);
			return ResponseEntity.ok(transitions);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@Operation(summary = "Get available transitions", description = "Retrieves all available transitions from the current state. "
			+ "Optionally filters by role. Only returns transitions that meet role requirements and conditions.")
	@ApiResponse(responseCode = "200", description = "List of available transitions")
	@PostMapping("/{taskId}/transitions/available")
	public ResponseEntity<List<Transition>> getAvailableTransitions(
			@Parameter(description = "task ID", required = true) @PathVariable String taskId,
			@Parameter(description = "User requesting available actions (transitions)") @RequestBody(required = true) TaskUserRequest user) {
		try {
			MachineEntity machine = machineService
					.getMachineByObject(taskId, MachineEntity.ManagedObjectType.REVIEW_TASK.toString())
					.orElseThrow(() -> new IllegalArgumentException("Task not found"));
			List<Transition> transitions = new ArrayList<Transition>();
			for (String role : user.getUserRoles()) {
				List<Transition> roleTransitions = machineService.getAvailableTransitions(machine.getId(), role);
				transitions.addAll(roleTransitions);
			}

			Map<String, Object> userRequesting = new HashMap<>();
			userRequesting.put("name", user.getUserName());
			userRequesting.put("email", user.getUserEmail());
			userRequesting.put("id", user.getUserId());

			// filter transitions
			List<Transition> result = transitions.stream()
					.filter(x -> !(isInContext(machine.getContext(), userRequesting, x.getSapuCtxList())
							&& x.getSingleActionPerUser().equalsIgnoreCase(TRUE_STR)))
					.toList();

			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Exeecute a new task
	 * 
	 * @param request TaskReviewerRequest
	 * @return
	 */
	private ResponseEntity<TaskReviewResponse> executeTask(String taskId, String transitionId, String role,
			Map<String, Object> context) {
		try {
			MachineEntity machine = machineService
					.getMachineByObject(taskId, MachineEntity.ManagedObjectType.REVIEW_TASK.toString())
					.orElseThrow(() -> new IllegalArgumentException("Task not found"));
			machine = machineService.updateContext(machine.getId(), (context != null ? context : machine.getContext()),
					role);

			machine = machineService.executeTransition(machine.getId(), transitionId);

			return ResponseEntity.ok(convertToResponse(machine));
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.notFound().build();
		}

	}

	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Review successfully", content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found") })
	@PostMapping("/{taskId}/review")
	public ResponseEntity<TaskReviewResponse> review(@RequestBody TaskUserRequest request) {
		try {
			MachineEntity machine = machineService
					.getMachineByObject(request.getTaskId(), MachineEntity.ManagedObjectType.REVIEW_TASK.toString())
					.orElseThrow(() -> new IllegalArgumentException("Task not found"));

			// // Prepare the new reviewer entry
			Map<String, Object> newReviewer = new HashMap<>();
			newReviewer.put("name", request.getUserName());
			newReviewer.put("email", request.getUserEmail());
			newReviewer.put("id", request.getUserId());

			Map<String, Object> initialContext = getNewMap(machine.getContext());

			addToContext(initialContext, newReviewer, "reviewers");
			// Update and save the context in the machine instance
			machine = machineService.updateContext(machine.getId(), initialContext, "reviewer");
			// Execute transition after context is saved
			machine = machineService.executeTransition(machine.getId(), "r_t0");
			return ResponseEntity.ok(convertToResponse(machine));

		} catch (Exception e) {
			log.error("Error adding review", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	private TaskReviewResponse convertToResponse(MachineEntity machine) {
		return new TaskReviewResponse(machine.getId(), machine.getMachineDefinitionId(), machine.getCurrentStateId(),
				machine.getContext(), machine.getCreatedAt(), machine.getUpdatedAt());
	}
}
