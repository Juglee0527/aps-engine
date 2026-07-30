package com.github.juglee0527.apsengine.scheduling;

import java.util.Comparator;

public class ShortestProcessingTimeRule implements SchedulingPriorityRule {

    private static final Comparator<SchedulingOrderInput> COMPARATOR =
            Comparator.comparingLong(
                            ShortestProcessingTimeRule::totalProcessingMinutes
                    )
                    .thenComparing(SchedulingOrderInput::dueAt)
                    .thenComparing(
                            SchedulingOrderInput::priority,
                            Comparator.reverseOrder()
                    )
                    .thenComparingLong(SchedulingOrderInput::orderId);

    @Override
    public int compare(
            SchedulingOrderInput left,
            SchedulingOrderInput right
    ) {
        return COMPARATOR.compare(left, right);
    }

    private static long totalProcessingMinutes(
            SchedulingOrderInput order
    ) {
        long minutesPerUnit = 0;
        try {
            for (SchedulingOperationInput operation : order.operations()) {
                minutesPerUnit = Math.addExact(
                        minutesPerUnit,
                        operation.processingTimeMinutesPerUnit()
                );
            }
            return Math.multiplyExact(order.quantity(), minutesPerUnit);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "SPT 비교를 위한 총 가공시간이 허용 범위를 초과합니다.",
                    exception
            );
        }
    }
}
