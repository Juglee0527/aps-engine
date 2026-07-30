package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;

public record SchedulingOrderInput(
        long orderId,
        String orderNumber,
        long productId,
        long quantity,
        OffsetDateTime releaseAt,
        OffsetDateTime dueAt,
        int priority,
        List<SchedulingOperationInput> operations
) {

    public SchedulingOrderInput {
        if (orderId < 1 || productId < 1) {
            throw new IllegalArgumentException(
                    "생산오더와 품목 식별자는 1 이상이어야 합니다."
            );
        }
        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "생산수량은 1 이상이어야 합니다."
            );
        }
        if (releaseAt == null || dueAt == null
                || !dueAt.isAfter(releaseAt)) {
            throw new IllegalArgumentException(
                    "납기시각은 투입 가능시각보다 이후여야 합니다."
            );
        }
        if (priority < 1) {
            throw new IllegalArgumentException(
                    "우선순위는 1 이상이어야 합니다."
            );
        }
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException(
                    "스케줄링할 공정은 하나 이상이어야 합니다."
            );
        }
        operations = List.copyOf(operations);
    }
}
