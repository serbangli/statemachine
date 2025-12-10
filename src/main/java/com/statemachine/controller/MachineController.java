package com.statemachine.controller;

import com.statemachine.domain.model.Transition;
import com.statemachine.dto.CreateMachineRequest;
import com.statemachine.dto.MachineResponse;
import com.statemachine.dto.TransitionRequest;
import com.statemachine.dto.UpdateContextRequest;
import com.statemachine.domain.entity.MachineEntity;
import com.statemachine.domain.entity.MachineHistoryEntity;
import com.statemachine.service.MachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    @Autowired
    private MachineService machineService;

    @PostMapping
    public ResponseEntity<MachineResponse> createMachine(@RequestBody CreateMachineRequest request) {
        try {
            MachineEntity machine = machineService.createMachineInstance(
                request.getMachineDefinitionId(),
                request.getInitialContext()
            );
            return ResponseEntity.ok(convertToResponse(machine));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{machineId}")
    public ResponseEntity<MachineResponse> getMachine(@PathVariable Long machineId) {
        return machineService.getMachine(machineId)
            .map(machine -> ResponseEntity.ok(convertToResponse(machine)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<MachineResponse>> getAllMachines() {
        List<MachineEntity> machines = machineService.getAllMachines();
        List<MachineResponse> responses = machines.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/definition/{machineDefinitionId}")
    public ResponseEntity<List<MachineResponse>> getMachinesByDefinition(@PathVariable String machineDefinitionId) {
        List<MachineEntity> machines = machineService.getMachinesByDefinition(machineDefinitionId);
        List<MachineResponse> responses = machines.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{machineId}/transitions/execute")
    public ResponseEntity<MachineResponse> executeTransition(
            @PathVariable Long machineId,
            @RequestBody TransitionRequest request) {
        try {
            MachineEntity machine = machineService.executeTransition(machineId, request.getTransitionId());
            return ResponseEntity.ok(convertToResponse(machine));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{machineId}/transitions/available")
    public ResponseEntity<List<Transition>> getAvailableTransitions(@PathVariable Long machineId) {
        try {
            List<Transition> transitions = machineService.getAvailableTransitions(machineId);
            return ResponseEntity.ok(transitions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{machineId}/context")
    public ResponseEntity<MachineResponse> updateContext(
            @PathVariable Long machineId,
            @RequestBody UpdateContextRequest request) {
        try {
            MachineEntity machine = machineService.updateContext(machineId, request.getContext());
            return ResponseEntity.ok(convertToResponse(machine));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{machineId}/history")
    public ResponseEntity<List<MachineHistoryEntity>> getMachineHistory(
            @PathVariable Long machineId,
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
            machine.getCurrentStateId(),
            machine.getContext(),
            machine.getCreatedAt(),
            machine.getUpdatedAt()
        );
    }
}

