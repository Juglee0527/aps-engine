package com.github.juglee0527.apsengine.order;

import java.time.OffsetDateTime;

public record ProductionOrderResponse(
        Long id,
        String orderNumber,
        Long productId,
        Long routingId,
        long quantity,
        OffsetDateTime releaseAt,
        OffsetDateTime dueAt,
        int priority,
        ProductionOrderStatus status
) {

    public static ProductionOrderResponse from(ProductionOrder order) {
        return new ProductionOrderResponse(
                order.id(),
                order.orderNumber(),
                order.routing().product().id(),
                order.routing().id(),
                order.quantity(),
                order.releaseAt(),
                order.dueAt(),
                order.priority(),
                order.status()
        );
    }
}
