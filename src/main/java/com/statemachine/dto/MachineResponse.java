package com.statemachine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineResponse {
    private Long id;
    private String machineDefinitionId;
    private String currentStateId;
    private Map<String, Object> context;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

