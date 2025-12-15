package com.statemachine.controller;

import com.statemachine.domain.entity.MachineDefinitionEntity;
import com.statemachine.service.MachineDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/machine-definitions")
@Tag(name = "Machine Definitions", description = "API for managing state machine definitions")
public class MachineDefinitionController {

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    @Operation(
        summary = "Load machine definition from file",
        description = "Loads a machine definition from an XML file in the classpath machines directory"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Machine definition loaded successfully",
            content = @Content(schema = @Schema(implementation = MachineDefinitionEntity.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or XML format")
    })
    @PostMapping("/load-from-file")
    public ResponseEntity<MachineDefinitionEntity> loadFromFile(
            @Parameter(description = "XML file name (e.g., 'task.xml')", required = true, example = "task.xml")
            @RequestParam String fileName) {
        try {
            MachineDefinitionEntity definition = machineDefinitionService.loadFromXml(fileName);
            return ResponseEntity.ok(definition);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);
        }
    }

    @Operation(
        summary = "Load machine definition from XML content",
        description = "Loads a machine definition from XML content provided in the request body"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Machine definition loaded successfully",
            content = @Content(schema = @Schema(implementation = MachineDefinitionEntity.class))),
        @ApiResponse(responseCode = "400", description = "Invalid XML content")
    })
    @PostMapping("/load-from-content")
    public ResponseEntity<MachineDefinitionEntity> loadFromContent(
            @Parameter(description = "Request body containing 'xmlContent' and 'machineId'", required = true)
            @RequestBody Map<String, String> request) {
        try {
            String xmlContent = request.get("xmlContent");
            String machineId = request.get("machineId");
            MachineDefinitionEntity definition = machineDefinitionService.loadFromXmlContent(xmlContent, machineId);
            return ResponseEntity.ok(definition);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);
        }
    }

    @Operation(
        summary = "Get machine definition by ID",
        description = "Retrieves a specific machine definition by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Machine definition found",
            content = @Content(schema = @Schema(implementation = MachineDefinitionEntity.class))),
        @ApiResponse(responseCode = "404", description = "Machine definition not found")
    })
    @GetMapping("/{machineDefinitionId}")
    public ResponseEntity<MachineDefinitionEntity> getDefinition(
            @Parameter(description = "Machine definition ID", required = true)
            @PathVariable String machineDefinitionId) {
        return machineDefinitionService.getDefinition(machineDefinitionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get all machine definitions",
        description = "Retrieves all machine definitions in the system"
    )
    @ApiResponse(responseCode = "200", description = "List of all machine definitions")
    @GetMapping
    public ResponseEntity<List<MachineDefinitionEntity>> getAllDefinitions() {
        return ResponseEntity.ok(machineDefinitionService.getAllDefinitions());
    }
}

