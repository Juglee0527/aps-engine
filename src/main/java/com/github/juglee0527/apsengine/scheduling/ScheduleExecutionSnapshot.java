package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

record ScheduleExecutionSnapshot(
        Long id,
        UUID executionKey,
        OffsetDateTime planningStart,
        DispatchingRule dispatchingRule,
        Long sourceScheduleRunId,
        OffsetDateTime frozenAt
) {
}
