package com.github.juglee0527.apsengine.scheduling;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlannedLeadTimeController {

    private final PlannedLeadTimeService plannedLeadTimeService;

    public PlannedLeadTimeController(
            PlannedLeadTimeService plannedLeadTimeService
    ) {
        this.plannedLeadTimeService = plannedLeadTimeService;
    }

    @GetMapping("/api/v1/schedules/{scheduleRunId}/lead-times")
    public List<PlannedLeadTime> calculate(
            @PathVariable long scheduleRunId
    ) {
        return plannedLeadTimeService.calculate(scheduleRunId);
    }
}
