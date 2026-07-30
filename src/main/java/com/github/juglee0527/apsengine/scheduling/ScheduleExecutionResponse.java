package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleExecutionResponse(
        Long id,
        UUID executionKey,
        ScheduleExecutionStatus status,
        OffsetDateTime planningStart,
        int planningOffsetSeconds,
        DispatchingRule dispatchingRule,
        Long sourceScheduleRunId,
        OffsetDateTime frozenAt,
        Long resultScheduleRunId,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {

    static ScheduleExecutionResponse from(
            ScheduleExecution execution
    ) {
        return new ScheduleExecutionResponse(
                execution.id(),
                execution.executionKey(),
                execution.status(),
                execution.planningStart(),
                execution.planningOffsetSeconds(),
                execution.dispatchingRule(),
                execution.sourceScheduleRunId(),
                execution.frozenAt(),
                execution.resultScheduleRunId(),
                execution.failureReason(),
                execution.createdAt(),
                execution.startedAt(),
                execution.completedAt()
        );
    }
}
