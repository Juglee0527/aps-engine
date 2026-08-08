package com.github.juglee0527.apsengine.learning;

import java.net.URI;
import java.util.List;

import com.github.juglee0527.apsengine.scheduling.ScheduleExecutionResponse;
import com.github.juglee0527.apsengine.scheduling.ScheduleExecutionService;
import com.github.juglee0527.apsengine.scheduling.DispatchingRuleComparisonResponse;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunService;
import com.github.juglee0527.apsengine.scheduling.ConstraintImpactResponse;

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
    private final ScheduleExecutionService executionService;
    private final ScheduleRunService scheduleRunService;
    private final FrozenHorizonLabService frozenHorizonLabService;
    private final LearningResultCoach resultCoach;

    public LearningScenarioController(
            LearningScenarioService service,
            ScheduleExecutionService executionService,
            ScheduleRunService scheduleRunService,
            FrozenHorizonLabService frozenHorizonLabService,
            LearningResultCoach resultCoach
    ) {
        this.service = service;
        this.executionService = executionService;
        this.scheduleRunService = scheduleRunService;
        this.frozenHorizonLabService = frozenHorizonLabService;
        this.resultCoach = resultCoach;
    }

    @GetMapping("/scenarios")
    public List<LearningScenarioDefinition> findScenarios() {
        return service.findScenarios();
    }

    @GetMapping("/scenarios/{scenarioKey}/coach")
    public LearningResultCoachResponse getCoach(
            @PathVariable String scenarioKey
    ) {
        return resultCoach.get(scenarioKey);
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

    @PostMapping("/instances/{instanceId}/schedules")
    public ResponseEntity<ScheduleExecutionResponse> schedule(
            @PathVariable long instanceId,
            @Valid @RequestBody LearningScheduleRequest request
    ) {
        LearningScenarioPlanScope scope = service.planScope(instanceId);
        ScheduleExecutionResponse execution = executionService.submit(
                request.executionKey(),
                scope.planningStart(),
                request.dispatchingRule(),
                scope.productionOrderIds()
        );
        service.trackScheduleExecution(instanceId, execution.id());
        return ResponseEntity.accepted()
                .location(URI.create(
                        "/api/v1/schedules/executions/" + execution.id()
                ))
                .body(execution);
    }

    @GetMapping("/instances/{instanceId}/rule-comparison")
    public DispatchingRuleComparisonResponse compareRules(
            @PathVariable long instanceId
    ) {
        LearningScenarioPlanScope scope = service.planScope(instanceId);
        return scheduleRunService.compareDispatchingRules(
                scope.planningStart(),
                scope.productionOrderIds()
        );
    }

    @GetMapping("/instances/{instanceId}/constraint-impact")
    public ConstraintImpactResponse compareConstraintImpact(
            @PathVariable long instanceId
    ) {
        LearningScenarioPlanScope scope = service.planScope(instanceId);
        return scheduleRunService.compareConstraintImpact(
                scope.instance().scenarioKey(),
                scope.planningStart(),
                scope.productionOrderIds()
        );
    }

    @PostMapping("/instances/{instanceId}/frozen-horizon")
    public FrozenHorizonLabResponse runFrozenHorizon(
            @PathVariable long instanceId,
            @Valid @RequestBody FrozenHorizonLabRequest request
    ) {
        return frozenHorizonLabService.run(instanceId, request);
    }
}
