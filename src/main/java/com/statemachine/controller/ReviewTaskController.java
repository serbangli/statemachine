package com.statemachine.controller;

import com.statemachine.domain.model.Transition;
import com.statemachine.dto.CreateMachineRequest;
import com.statemachine.dto.MachineResponse;
import com.statemachine.dto.TransitionRequest;
import com.statemachine.dto.task.TaskReviewRequest;
import com.statemachine.dto.task.TaskReviewerRequest;
import com.statemachine.dto.task.TaskReviewResponse;
import com.statemachine.dto.UpdateContextRequest;
import com.statemachine.domain.entity.MachineEntity;
import com.statemachine.domain.entity.MachineHistoryEntity;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "ReviewTasks", description = "API for managing review tasks")
public class ReviewTaskController {

    @Autowired
    private MachineService machineService;

    @Operation(
        summary = "Create a new task review machine instance",
        description = "Creates a new task review machine instance from a machine definition with an initial context"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task review machine instance created successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping
    public ResponseEntity<TaskReviewResponse> createTask(@RequestBody TaskReviewRequest request) {
        try {
            Map<String, Object> initialContext = new HashMap<>();
            initialContext.put("ownerId", request.getOwnerId());            
            
            MachineEntity machine = machineService.createMachineInstance(
                request.getMachineDefinitionId(),
                initialContext,
                request.getTaskId(),
                MachineEntity.ManagedObjectType.REVIEW_TASK.toString()
            );
            return ResponseEntity.ok(convertToResponse(machine));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(
        summary = "Add a new reviewer to a task",
        description = "Adds a new reviewer to a task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviewer added successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/reviewer")
    public ResponseEntity<TaskReviewResponse> addReviewer(@RequestBody TaskReviewerRequest request) {
        try {
            
        List<Map<String, Object>> reviewers = new ArrayList<>();
        reviewers.add(new HashMap<String, Object>() {{ put("name", request.getReviewerName());
            put("email", request.getReviewerEmail());
            put("id", request.getReviewerId());
         }});                
            
            MachineEntity machine = machineService.getMachineByObject(request.getTaskId(), 
            MachineEntity.ManagedObjectType.REVIEW_TASK.toString()).orElseThrow(() -> new IllegalArgumentException("Task not found"));
            Map<String, Object> oldCtx = machine.getContext(); 
            if (oldCtx.entrySet().contains("reviewers")) {
                List<Map<String, Object>> olds = (List<Map<String, Object>>) oldCtx.get("reqreviewers");
                olds.addAll(reviewers);
                 oldCtx.put("reqreviewers", olds);
            } else {
                oldCtx.put("reqreviewers", reviewers);
            }
            try {
                machine = machineService.updateContext(machine.getId(), oldCtx, "owner");
                machineService.executeTransition(machine.getId(), "o_t0");
                return ResponseEntity.ok(convertToResponse(machine));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(
        summary = "Owner closes review task",
        description = "Owner closes review task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review task closed successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/review/close")
    public ResponseEntity<TaskReviewResponse> closeReview(@RequestBody TaskReviewerRequest request) {
        return executeTask(request,"o_t1","owner",null);
    }
    @Operation(
        summary = "Owner reopens review task",
        description = "Owner reopens review task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review task reopened successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/review/reopen")
    public ResponseEntity<TaskReviewResponse> reopenReview(@RequestBody TaskReviewerRequest request) {
        return executeTask(request,"o_t2","owner",null);        
    }

    @Operation(
        summary = "Owner rolls back review task",
        description = "Owner rolls back review task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review task rolled back successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/review/rollback")
    public ResponseEntity<TaskReviewResponse> rollbackReview(@RequestBody TaskReviewerRequest request) {
        Map<String, Object> context = new HashMap<>();
        List<Map<String, Object>> reviewers = new ArrayList<>();          
        context.put("reviewers", reviewers);
        return executeTask(request,"o_t3","owner",context);        
    }

    @Operation(
        summary = "Owner start integrating review task ",
        description = "Owner start integrating review task into content"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review task start integrating successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/integration/start")
    public ResponseEntity<TaskReviewResponse> startIntegration(@RequestBody TaskReviewerRequest request) {
        Map<String, Object> context = new HashMap<>();
        List<Map<String, Object>> reviewers = new ArrayList<>();          
        context.put("reviewers", reviewers);
        return executeTask(request,"o_t4","owner",context);        
    }

    @Operation(
        summary = "Owner cancel integrating review task ",
        description = "Owner cancel integrating review task into content"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review task cancel integrating successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/integration/cancel")
    public ResponseEntity<TaskReviewResponse> cancelIntegration(@RequestBody TaskReviewerRequest request) {
        Map<String, Object> context = new HashMap<>();
        List<Map<String, Object>> reviewers = new ArrayList<>();          
        context.put("reviewers", reviewers);
        return executeTask(request,"o_t5","owner",context);        
    }

    @Operation(
        summary = "Owner publish review task ",
        description = "Owner publish review task into content"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review task publish successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/integration/finish")
    public ResponseEntity<TaskReviewResponse> finishIntegration(@RequestBody TaskReviewerRequest request) {
        Map<String, Object> context = new HashMap<>();
        List<Map<String, Object>> reviewers = new ArrayList<>();          
        context.put("reviewers", reviewers);
        return executeTask(request,"o_t6","owner",context);        
    }



/**
 * Exeecute a new task
 * @param request TaskReviewerRequest
 * @return
 */
    ResponseEntity<TaskReviewResponse> executeTask(TaskReviewerRequest request, String transitionId, String role, Map<String, Object> context) {
        try {
            MachineEntity machine = machineService.getMachineByObject(request.getTaskId(), MachineEntity.ManagedObjectType.REVIEW_TASK.toString()).orElseThrow(() -> new IllegalArgumentException("Task not found"));
            try {
                machine = machineService.updateContext(machine.getId(), (context != null ? context : machine.getContext()), role);
                machineService.executeTransition(machine.getId(),  transitionId);
                return ResponseEntity.ok(convertToResponse(machine));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.notFound().build();
            }

          
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping("/{taskId}/review")
    public ResponseEntity<TaskReviewResponse> review(@RequestBody TaskReviewerRequest request) {
        try {
            
            List<Map<String, Object>> reviewers = new ArrayList<>();
            reviewers.add(new HashMap<String, Object>() {{ put("name", request.getReviewerName());
                put("email", request.getReviewerEmail());
                put("id", request.getReviewerId());
             }});                
                
                MachineEntity machine = machineService.getMachineByObject(request.getTaskId(), MachineEntity.ManagedObjectType.REVIEW_TASK.toString()).orElseThrow(() -> new IllegalArgumentException("Task not found"));
                Map<String, Object> oldCtx = machine.getContext(); 
                if (oldCtx.entrySet().contains("reviewers")) {
                    List<Map<String, Object>> olds = (List<Map<String, Object>>) oldCtx.get("reviewers");
                    olds.addAll(reviewers);
                     oldCtx.put("reviewers", olds);
                } else {
                    oldCtx.put("reviewers", reviewers);
                }
                try {
                   machine = machineService.updateContext(machine.getId(), oldCtx, "reviewer");
                    machineService.executeTransition(machine.getId(), "r_t0");
                    return ResponseEntity.ok(convertToResponse(machine));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.notFound().build();
                }
    
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
    }

    @Operation(
        summary = "Get a machine instance by ID",
        description = "Retrieves a specific machine instance by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Machine instance found",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "404", description = "Machine instance not found")
    })
    @GetMapping("/{taskId}/")
    public ResponseEntity<TaskReviewResponse> getMachine(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId) {
        return machineService.getMachineByObject(taskId, MachineEntity.ManagedObjectType.REVIEW_TASK.toString())
            .map(machine -> ResponseEntity.ok(convertToResponse(machine)))
            .orElse(ResponseEntity.notFound().build());
    }


    @Operation(
        summary = "Get all machine instances",
        description = "Retrieves all machine instances in the system"
    )
    @ApiResponse(responseCode = "200", description = "List of all machine instances")
    @GetMapping
    public ResponseEntity<List<TaskReviewResponse>> getAllMachines() {
        List<MachineEntity> machines = machineService.getAllMachines();
        List<TaskReviewResponse> responses = machines.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    

    @GetMapping("/definition/{machineDefinitionId}")
    public ResponseEntity<List<TaskReviewResponse>> getMachinesByDefinition(
            @Parameter(description = "Machine definition ID", required = true)
            @PathVariable String machineDefinitionId) {
        List<MachineEntity> machines = machineService.getMachinesByDefinition(machineDefinitionId);
        List<TaskReviewResponse> responses = machines.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Execute a transition",
        description = "Executes a transition on a machine instance, moving it from the current state to the destination state"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transition executed successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transition or condition not met")
    })
    @PostMapping("/{machineId}/transitions/execute")
    public ResponseEntity<TaskReviewResponse> executeTransition(
            @Parameter(description = "Machine instance ID", required = true)
            @PathVariable Long machineId,
            @RequestBody TransitionRequest request) {
        try {
            MachineEntity machine = machineService.executeTransition(machineId, request.getTransitionId());
            return ResponseEntity.ok(convertToResponse(machine));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(
        summary = "Get available transitions",
        description = "Retrieves all available transitions from the current state. " +
            "Optionally filters by role. Only returns transitions that meet role requirements and conditions."
    )
    @ApiResponse(responseCode = "200", description = "List of available transitions")
    @GetMapping("/{machineId}/transitions/available")
    public ResponseEntity<List<Transition>> getAvailableTransitions(
            @Parameter(description = "Machine instance ID", required = true)
            @PathVariable Long machineId,
            @Parameter(description = "Optional role filter (e.g., 'owner', 'reviewer')")
            @RequestParam(required = false) String role) {
        try {
            List<Transition> transitions = machineService.getAvailableTransitions(machineId, role);
            return ResponseEntity.ok(transitions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get all transitions from current state",
        description = "Retrieves all transitions from the current state without filtering by role or conditions. " +
            "Useful for displaying all possible transitions grouped by role."
    )
    @ApiResponse(responseCode = "200", description = "List of all transitions from current state")
    @GetMapping("/{machineId}/transitions/all")
    public ResponseEntity<List<Transition>> getAllTransitionsFromCurrentState(
            @Parameter(description = "Machine instance ID", required = true)
            @PathVariable Long machineId) {
        try {
            List<Transition> transitions = machineService.getAllTransitionsFromCurrentState(machineId);
            return ResponseEntity.ok(transitions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Update machine context",
        description = "Updates the context of a machine instance. " +
            "If only one transition becomes available after the update, it will be automatically executed. " +
            "If multiple transitions become available, a warning is logged and user must select."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Context updated successfully",
            content = @Content(schema = @Schema(implementation = TaskReviewResponse.class))),
        @ApiResponse(responseCode = "404", description = "Machine instance not found")
    })
    @PutMapping("/{machineId}/context")
    public ResponseEntity<TaskReviewResponse> updateContext(
            @Parameter(description = "Machine instance ID", required = true)
            @PathVariable Long machineId,
            @RequestBody UpdateContextRequest request,
            @RequestParam(required = false) String role){
        try {
            MachineEntity machine = machineService.updateContext(machineId, request.getContext(), role);
            return ResponseEntity.ok(convertToResponse(machine));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get machine history",
        description = "Retrieves the transition history for a machine instance, ordered by timestamp"
    )
    @ApiResponse(responseCode = "200", description = "List of machine history entries")
    @GetMapping("/{machineId}/history")
    public ResponseEntity<List<MachineHistoryEntity>> getMachineHistory(
            @Parameter(description = "Machine instance ID", required = true)
            @PathVariable Long machineId,
            @Parameter(description = "Sort order: 'asc' (oldest first) or 'desc' (newest first)", example = "asc")
            @RequestParam(required = false, defaultValue = "asc") String order) {
        try {
            List<MachineHistoryEntity> history;
            if ("desc".equalsIgnoreCase(order)) {
                history = machineService.getMachineHistoryDesc(machineId);
            } else {
                history = machineService.getMachineHistory(machineId);
            }
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private TaskReviewResponse convertToResponse(MachineEntity machine) {
        return new TaskReviewResponse(
            machine.getId(),
            machine.getMachineDefinitionId(),
            machine.getCurrentStateId(),
            machine.getContext(),
            machine.getCreatedAt(),
            machine.getUpdatedAt()
        );
    }
}

