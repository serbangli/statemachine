package com.statemachine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to execute a transition")
public class TransitionRequest {
    @Schema(description = "Transition ID to execute", example = "t0", requiredMode = Schema.RequiredMode.REQUIRED)
    private String transitionId;
}

