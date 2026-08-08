package com.github.juglee0527.apsengine.learning;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningScenarioController {

    private final LearningScenarioService service;

    public LearningScenarioController(LearningScenarioService service) {
        this.service = service;
    }

    @GetMapping("/scenarios")
    public List<LearningScenarioDefinition> findScenarios() {
        return service.findScenarios();
    }

    @PostMapping("/scenarios/{scenarioKey}/instances")
    public ResponseEntity<LearningScenarioInstanceResponse> create(
            @PathVariable String scenarioKey,
            @Valid @RequestBody LearningScenarioCreateRequest request
    ) {
        LearningScenarioInstanceResponse response = service.create(
                scenarioKey,
                request.requestKey()
        );
        return ResponseEntity.created(URI.create(
                "/api/v1/learning/instances/" + response.id()
        )).body(response);
    }

    @GetMapping("/instances/{instanceId}")
    public LearningScenarioInstanceResponse find(
            @PathVariable long instanceId
    ) {
        return service.find(instanceId);
    }

    @DeleteMapping("/instances/{instanceId}")
    public LearningScenarioInstanceResponse reset(
            @PathVariable long instanceId
    ) {
        return service.reset(instanceId);
    }
}
