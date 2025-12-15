package com.statemachine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new machine instance")
public class CreateMachineRequest {
    @Schema(description = "Machine definition ID", example = "task", required = true)
    private String machineDefinitionId;
    
    @Schema(description = "Initial context for the machine instance (JSON object)", 
            example = "{\"name\": \"john\", \"role\": \"owner\"}")
    private Map<String, Object> initialContext;
}

