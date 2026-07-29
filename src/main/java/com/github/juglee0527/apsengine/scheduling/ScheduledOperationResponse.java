package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;

public record ScheduledOperationResponse(
        Long id,
        Long productionOrderId,
        String orderNumber,
        Long productId,
        String productCode,
        String productName,
        Long operationId,
        int sequence,
        String operationCode,
        String operationName,
        Long machineId,
        String machineCode,
        String machineName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long workingMinutes,
        OffsetDateTime dueAt,
        boolean delayed
) {

    public static ScheduledOperationResponse from(
            ScheduledOperation scheduledOperation
    ) {
        var order = scheduledOperation.productionOrder();
        var product = order.routing().product();
        var operation = scheduledOperation.operation();
        var machine = scheduledOperation.machine();
        return new ScheduledOperationResponse(
                scheduledOperation.id(),
                order.id(),
                order.orderNumber(),
                product.id(),
                product.code(),
                product.name(),
                operation.id(),
                scheduledOperation.sequence(),
                operation.code(),
                operation.name(),
                machine.id(),
                machine.code(),
                machine.name(),
                scheduledOperation.startAt(),
                scheduledOperation.endAt(),
                scheduledOperation.workingMinutes(),
                order.dueAt(),
                scheduledOperation.delayed()
        );
    }
}
