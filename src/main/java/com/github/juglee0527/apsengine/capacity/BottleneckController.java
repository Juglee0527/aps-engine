package com.github.juglee0527.apsengine.capacity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BottleneckController {

    private final BottleneckService bottleneckService;

    public BottleneckController(BottleneckService bottleneckService) {
        this.bottleneckService = bottleneckService;
    }

    @GetMapping("/api/v1/schedules/{scheduleRunId}/bottlenecks")
    public BottleneckAnalysis detect(
            @PathVariable long scheduleRunId
    ) {
        return bottleneckService.detect(scheduleRunId);
    }
}
