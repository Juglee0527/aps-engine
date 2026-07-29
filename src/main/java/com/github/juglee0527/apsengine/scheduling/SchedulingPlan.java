package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;

public record SchedulingPlan(
        OffsetDateTime planningStart,
        OffsetDateTime schedulingEnd,
        List<ScheduledTask> tasks
) {

    public SchedulingPlan {
        tasks = List.copyOf(tasks);
    }
}
