package com.statemachine.controller;

import com.statemachine.domain.model.Transition;
import com.statemachine.dto.CreateMachineRequest;
import com.statemachine.dto.MachineResponse;
import com.statemachine.dto.TransitionRequest;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/machines")
@Tag(name = "Machine Instances", description = "API for managing state machine instances")
public class MachineController {

    @Autowired
    private MachineService machineService;

    @Operation(
        summary = "Create a new machine instance",
        description = "Creates a new state machine instance from a machine definition with an initial context"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Machine instance created successfully",
            content = @Content(schema = @Schema(implementation = MachineResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or machine definition not found")
    })
    @PostMapping
    public ResponseEntity<MachineResponse> createMachine(@RequestBody CreateMachineRequest request) {
        try {
            MachineEntity machine = machineService.createMachineInstance(
                request.getMachineDefinitionId(),
                request.getInitialContext(), null,null
            );
            return ResponseEntity.ok(convertToResponse(machine));
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
            content = @Content(schema = @Schema(implementation = MachineResponse.class))),
        @ApiResponse(responseCode = "404", description = "Machine instance not found")
    })
    @GetMapping("/{machineId}")
    public ResponseEntity<MachineResponse> getMachine(
            @Parameter(description = "Machine instance ID", required = true)
            @PathVariable Long machineId) {
        return machineService.getMachine(machineId)
            .map(machine -> ResponseEntity.ok(convertToResponse(machine)))
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get all machine instances",
        description = "Retrieves all machine instances in the system"
    )
    @ApiResponse(responseCode = "200", description = "List of all machine instances")
    @GetMapping
    public ResponseEntity<List<MachineResponse>> getAllMachines() {
        List<MachineEntity> machines = machineService.getAllMachines();
        List<MachineResponse> responses = machines.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get machines by definition ID",
        description = "Retrieves all machine instances that use a specific machine definition"
    )
    @ApiResponse(responseCode = "200", description = "List of machine instances for the definition")
    @GetMapping("/definition/{machineDefinitionId}")
    public ResponseEntity<List<MachineResponse>> getMachinesByDefinition(
            @Parameter(description = "Machine definition ID", required = true)
            @PathVariable String machineDefinitionId) {
        List<MachineEntity> machines = machineService.getMachinesByDefinition(machineDefinitionId);
        List<MachineResponse> responses = machines.stream()
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
            content = @Content(schema = @Schema(implementation = MachineResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transition or condition not met")
    })
    @PostMapping("/{machineId}/transitions/execute")
    public ResponseEntity<MachineResponse> executeTransition(
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
            content = @Content(schema = @Schema(implementation = MachineResponse.class))),
        @ApiResponse(responseCode = "404", description = "Machine instance not found")
    })
    @PutMapping("/{machineId}/context")
    public ResponseEntity<MachineResponse> updateContext(
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

    private MachineResponse convertToResponse(MachineEntity machine) {
        return new MachineResponse(
            machine.getId(),
            machine.getMachineDefinitionId(),
            null,
            null,
            machine.getCurrentStateId(),
            machine.getContext(),
            machine.getCreatedAt(),
            machine.getUpdatedAt()
        );
    }
}

