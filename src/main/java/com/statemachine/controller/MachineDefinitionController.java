package com.statemachine.controller;

import com.statemachine.domain.entity.MachineDefinitionEntity;
import com.statemachine.service.MachineDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/machine-definitions")
public class MachineDefinitionController {

    @Autowired
    private MachineDefinitionService machineDefinitionService;

    @PostMapping("/load-from-file")
    public ResponseEntity<MachineDefinitionEntity> loadFromFile(@RequestParam String fileName) {
        try {
            MachineDefinitionEntity definition = machineDefinitionService.loadFromXml(fileName);
            return ResponseEntity.ok(definition);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);
        }
    }

    @PostMapping("/load-from-content")
    public ResponseEntity<MachineDefinitionEntity> loadFromContent(@RequestBody Map<String, String> request) {
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

    @GetMapping("/{machineDefinitionId}")
    public ResponseEntity<MachineDefinitionEntity> getDefinition(@PathVariable String machineDefinitionId) {
        return machineDefinitionService.getDefinition(machineDefinitionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<MachineDefinitionEntity>> getAllDefinitions() {
        return ResponseEntity.ok(machineDefinitionService.getAllDefinitions());
    }
}

