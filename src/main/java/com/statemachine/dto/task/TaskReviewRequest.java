package com.statemachine.dto.task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a task")
public class TaskReviewRequest {
    @Schema(description = "Task ID", example = "1")
    private String taskId;

    @Schema(description = "Owner ID", example = "john@example.com")
    private String ownerId;

    @Schema(description = "Machine definition ID", example = "task", requiredMode = Schema.RequiredMode.REQUIRED)
    private String machineDefinitionId;    
}
