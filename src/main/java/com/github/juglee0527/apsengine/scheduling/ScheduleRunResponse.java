package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ScheduleRunResponse(
        Long id,
        UUID executionKey,
        ScheduleRunStatus status,
        OffsetDateTime planningStart,
        OffsetDateTime schedulingEnd,
        int planningOffsetSeconds,
        OffsetDateTime createdAt,
        int orderCount,
        int taskCount,
        int delayedOrderCount,
        List<ScheduledOperationResponse> tasks
) {

    public static ScheduleRunResponse from(ScheduleRun scheduleRun) {
        List<ScheduledOperationResponse> tasks =
                scheduleRun.scheduledOperations()
                        .stream()
                        .map(ScheduledOperationResponse::from)
                        .toList();
        int orderCount = (int) tasks.stream()
                .map(ScheduledOperationResponse::productionOrderId)
                .distinct()
                .count();
        int delayedOrderCount = (int) tasks.stream()
                .filter(ScheduledOperationResponse::delayed)
                .map(ScheduledOperationResponse::productionOrderId)
                .distinct()
                .count();
        return new ScheduleRunResponse(
                scheduleRun.id(),
                scheduleRun.executionKey(),
                scheduleRun.status(),
                scheduleRun.planningStart(),
                scheduleRun.schedulingEnd(),
                scheduleRun.planningOffsetSeconds(),
                scheduleRun.createdAt(),
                orderCount,
                tasks.size(),
                delayedOrderCount,
                tasks
        );
    }
}
