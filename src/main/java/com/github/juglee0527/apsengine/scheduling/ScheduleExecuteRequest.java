package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ScheduleExecuteRequest(
        @NotNull UUID executionKey,
        @NotNull OffsetDateTime planningStart,
        DispatchingRule dispatchingRule,
        List<Long> productionOrderIds
) {

    public ScheduleExecuteRequest {
        dispatchingRule = dispatchingRule == null
                ? DispatchingRule.EXPLICIT_PRIORITY
                : dispatchingRule;
        productionOrderIds = productionOrderIds == null
                ? null
                : List.copyOf(productionOrderIds);
    }
}
