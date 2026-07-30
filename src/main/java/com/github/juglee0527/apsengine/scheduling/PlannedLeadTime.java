package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;

public record PlannedLeadTime(
        long productionOrderId,
        String orderNumber,
        long productId,
        String productCode,
        OffsetDateTime releaseAt,
        OffsetDateTime completionAt,
        long plannedLeadTimeMinutes,
        long processingMinutes,
        long changeoverMinutes,
        long waitingMinutes,
        int operationCount
) {
}
