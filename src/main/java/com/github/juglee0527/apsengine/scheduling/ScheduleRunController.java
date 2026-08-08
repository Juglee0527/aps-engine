package com.github.juglee0527.apsengine.scheduling;

import java.net.URI;
import java.util.List;

import com.github.juglee0527.apsengine.common.web.PageResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleRunController {

    private final ScheduleRunService scheduleRunService;
    private final ScheduleExecutionService executionService;
    private final ScheduledOperationRepository scheduledOperationRepository;

    public ScheduleRunController(
            ScheduleRunService scheduleRunService,
            ScheduleExecutionService executionService,
            ScheduledOperationRepository scheduledOperationRepository
    ) {
        this.scheduleRunService = scheduleRunService;
        this.executionService = executionService;
        this.scheduledOperationRepository = scheduledOperationRepository;
    }

    @PostMapping
    public ResponseEntity<ScheduleExecutionResponse> execute(
            @Valid @RequestBody ScheduleExecuteRequest request
    ) {
        ScheduleExecutionResponse execution = executionService.submit(
                request.executionKey(),
                request.planningStart(),
                request.dispatchingRule(),
                request.productionOrderIds()
        );
        return accepted(execution);
    }

    @PostMapping("/{scheduleRunId}/reschedule")
    public ResponseEntity<ScheduleExecutionResponse> reschedule(
            @PathVariable long scheduleRunId,
            @Valid @RequestBody ScheduleRescheduleRequest request
    ) {
        ScheduleExecutionResponse execution =
                executionService.submitReschedule(
                scheduleRunId,
                request.executionKey(),
                request.frozenAt(),
                request.dispatchingRule()
        );
        return accepted(execution);
    }

    @GetMapping("/executions")
    public List<ScheduleExecutionResponse> getExecutions(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return executionService.findRecent(limit);
    }

    @GetMapping("/executions/{executionId}")
    public ScheduleExecutionResponse getExecution(
            @PathVariable Long executionId
    ) {
        return executionService.find(executionId);
    }

    @GetMapping("/latest")
    public ScheduleRunResponse getLatest() {
        return ScheduleRunResponse.from(scheduleRunService.getLatest());
    }

    @GetMapping("/latest/summary")
    public ScheduleRunSummaryResponse getLatestSummary() {
        ScheduleRun run = scheduleRunService.getLatestSummary();
        return summary(run);
    }

    @GetMapping("/{scheduleRunId}")
    public ScheduleRunResponse getById(
            @PathVariable long scheduleRunId
    ) {
        return ScheduleRunResponse.from(
                scheduleRunService.getById(scheduleRunId)
        );
    }

    @GetMapping("/{scheduleRunId}/tasks")
    public PageResponse<ScheduledOperationResponse> getTasks(
            @PathVariable long scheduleRunId,
            @Valid @ModelAttribute ScheduleTaskSearchParameters request
    ) {
        scheduleRunService.getSummaryById(scheduleRunId);
        Page<ScheduledOperation> page = scheduledOperationRepository.search(
                scheduleRunId,
                request.machineId(),
                request.from(),
                request.to(),
                request.query(),
                PageRequest.of(request.page(), request.size())
        );
        return PageResponse.from(page, ScheduledOperationResponse::from);
    }

    private ScheduleRunSummaryResponse summary(ScheduleRun run) {
        return ScheduleRunSummaryResponse.from(
                run,
                scheduledOperationRepository.countOrders(run.id()),
                scheduledOperationRepository.countByScheduleRun_Id(run.id())
        );
    }

    private ResponseEntity<ScheduleExecutionResponse> accepted(
            ScheduleExecutionResponse execution
    ) {
        URI location = URI.create(
                "/api/v1/schedules/executions/" + execution.id()
        );
        return ResponseEntity.accepted()
                .location(location)
                .body(execution);
    }
}
