package com.github.juglee0527.apsengine.scheduling;

import java.math.BigDecimal;
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
        Long sourceScheduleRunId,
        OffsetDateTime frozenAt,
        DispatchingRule dispatchingRule,
        int orderCount,
        int taskCount,
        long totalTardinessMinutes,
        int delayedOrderCount,
        long makespanMinutes,
        BigDecimal machineUtilizationPercent,
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
        return new ScheduleRunResponse(
                scheduleRun.id(),
                scheduleRun.executionKey(),
                scheduleRun.status(),
                scheduleRun.planningStart(),
                scheduleRun.schedulingEnd(),
                scheduleRun.planningOffsetSeconds(),
                scheduleRun.createdAt(),
                scheduleRun.sourceScheduleRunId(),
                scheduleRun.frozenAt(),
                scheduleRun.dispatchingRule(),
                orderCount,
                tasks.size(),
                scheduleRun.totalTardinessMinutes(),
                scheduleRun.delayedOrderCount(),
                scheduleRun.makespanMinutes(),
                scheduleRun.machineUtilizationPercent(),
                tasks
        );
    }
}
