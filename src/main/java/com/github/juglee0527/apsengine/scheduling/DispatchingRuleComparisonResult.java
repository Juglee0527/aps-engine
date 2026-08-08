package com.github.juglee0527.apsengine.scheduling;

import java.math.BigDecimal;
import java.util.List;

public record DispatchingRuleComparisonResult(
        DispatchingRule dispatchingRule,
        long totalTardinessMinutes,
        int delayedOrderCount,
        long makespanMinutes,
        BigDecimal machineUtilizationPercent,
        List<String> orderSequence,
        List<ScheduledTask> tasks
) {
    public DispatchingRuleComparisonResult {
        orderSequence = List.copyOf(orderSequence);
        tasks = List.copyOf(tasks);
    }
}
