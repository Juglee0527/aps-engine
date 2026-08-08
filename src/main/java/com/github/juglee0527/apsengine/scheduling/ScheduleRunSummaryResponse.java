package com.github.juglee0527.apsengine.scheduling;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ScheduleRunSummaryResponse(
        Long id,
        ScheduleRunStatus status,
        OffsetDateTime planningStart,
        OffsetDateTime schedulingEnd,
        int planningOffsetSeconds,
        Long sourceScheduleRunId,
        OffsetDateTime frozenAt,
        DispatchingRule dispatchingRule,
        long orderCount,
        long taskCount,
        long totalTardinessMinutes,
        int delayedOrderCount,
        long makespanMinutes,
        BigDecimal machineUtilizationPercent
) {
    static ScheduleRunSummaryResponse from(
            ScheduleRun run,
            long orderCount,
            long taskCount
    ) {
        return new ScheduleRunSummaryResponse(
                run.id(), run.status(), run.planningStart(),
                run.schedulingEnd(), run.planningOffsetSeconds(),
                run.sourceScheduleRunId(), run.frozenAt(),
                run.dispatchingRule(), orderCount, taskCount,
                run.totalTardinessMinutes(), run.delayedOrderCount(),
                run.makespanMinutes(), run.machineUtilizationPercent()
        );
    }
}
