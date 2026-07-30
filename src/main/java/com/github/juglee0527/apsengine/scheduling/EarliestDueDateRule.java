package com.github.juglee0527.apsengine.scheduling;

import java.util.Comparator;

public class EarliestDueDateRule implements SchedulingPriorityRule {

    private static final Comparator<SchedulingOrderInput> COMPARATOR =
            Comparator.comparing(SchedulingOrderInput::dueAt)
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
}
