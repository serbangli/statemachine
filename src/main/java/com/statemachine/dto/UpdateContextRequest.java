package com.statemachine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update machine context")
public class UpdateContextRequest {
    @Schema(description = "Context data to update (JSON object)", 
            example = "{\"users\": [{\"name\": \"john\", \"role\": \"owner\"}]}")
    private Map<String, Object> context;
}

