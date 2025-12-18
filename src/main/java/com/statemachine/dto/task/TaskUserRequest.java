package com.statemachine.dto.task;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to review a task")
public class TaskUserRequest {
    @Schema(description = "Task ID", example = "1")
    private String taskId;
    
    @Schema(description = "User ID", example = "1")
    private String userId;

    @Schema(description = "User name", example = "John Doe")
    private String userName;

    @Schema(description = "User email", example = "john.doe@example.com")
    private String userEmail;
    
    @Schema(description = "User roles", example = "[\"reviewer\", \"legal\", \"owner\"]")    
    private List<String> userRoles;
        
    
}
