package com.statemachine.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to review a task")
public class TaskReviewerRequest {
    @Schema(description = "Task ID", example = "1")
    private String taskId;
    
    @Schema(description = "Reviewer ID", example = "1")
    private String reviewerId;

    @Schema(description = "Reviewer name", example = "John Doe")
    private String reviewerName;

    @Schema(description = "Reviewer email", example = "john.doe@example.com")
    private String reviewerEmail;
}
