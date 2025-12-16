package com.statemachine.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Task review response")
public class TaskReviewResponse {
    @Schema(description = "Machine instance ID", example = "1")
    private Long machineId;
    
    @Schema(description = "Machine definition ID", example = "task")
    private String machineDefinitionId;
    
    @Schema(description = "Current state ID", example = "DRAFT")
    private String currentStateId;
    
    @Schema(description = "Machine context (JSON object)")
    private Map<String, Object> context;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}

