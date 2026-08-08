package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record ScheduleExecutionSnapshot(
        Long id,
        UUID executionKey,
        OffsetDateTime planningStart,
        DispatchingRule dispatchingRule,
        List<Long> productionOrderIds,
        Long sourceScheduleRunId,
        OffsetDateTime frozenAt
) {
    ScheduleExecutionSnapshot(
            Long id,
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule,
            Long sourceScheduleRunId,
            OffsetDateTime frozenAt
    ) {
        this(
                id,
                executionKey,
                planningStart,
                dispatchingRule,
                null,
                sourceScheduleRunId,
                frozenAt
        );
    }
}
