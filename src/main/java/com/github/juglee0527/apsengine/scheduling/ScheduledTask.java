package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;

public record ScheduledTask(
        long orderId,
        String orderNumber,
        long operationId,
        long machineId,
        int sequence,
        String operationCode,
        String operationName,
        OffsetDateTime changeoverStartAt,
        long changeoverMinutes,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long workingMinutes,
        OffsetDateTime dueAt,
        boolean delayed
) {
}
