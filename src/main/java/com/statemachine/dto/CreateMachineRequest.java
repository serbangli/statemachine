package com.statemachine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMachineRequest {
    private String machineDefinitionId;
    private Map<String, Object> initialContext;
}

