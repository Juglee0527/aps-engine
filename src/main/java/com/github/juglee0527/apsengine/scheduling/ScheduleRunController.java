package com.github.juglee0527.apsengine.scheduling;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleRunController {

    private final ScheduleRunService scheduleRunService;

    public ScheduleRunController(ScheduleRunService scheduleRunService) {
        this.scheduleRunService = scheduleRunService;
    }

    @PostMapping
    public ResponseEntity<ScheduleRunResponse> execute(
            @Valid @RequestBody ScheduleExecuteRequest request
    ) {
        ScheduleRun scheduleRun = scheduleRunService.execute(
                request.executionKey(),
                request.planningStart(),
                request.dispatchingRule()
        );
        return ResponseEntity.ok(ScheduleRunResponse.from(scheduleRun));
    }

    @PostMapping("/{scheduleRunId}/reschedule")
    public ResponseEntity<ScheduleRunResponse> reschedule(
            @PathVariable long scheduleRunId,
            @Valid @RequestBody ScheduleRescheduleRequest request
    ) {
        ScheduleRun scheduleRun = scheduleRunService.reschedule(
                scheduleRunId,
                request.executionKey(),
                request.frozenAt(),
                request.dispatchingRule()
        );
        return ResponseEntity.ok(ScheduleRunResponse.from(scheduleRun));
    }

    @GetMapping("/latest")
    public ScheduleRunResponse getLatest() {
        return ScheduleRunResponse.from(scheduleRunService.getLatest());
    }

    @GetMapping("/{scheduleRunId}")
    public ScheduleRunResponse getById(
            @PathVariable long scheduleRunId
    ) {
        return ScheduleRunResponse.from(
                scheduleRunService.getById(scheduleRunId)
        );
    }
}
