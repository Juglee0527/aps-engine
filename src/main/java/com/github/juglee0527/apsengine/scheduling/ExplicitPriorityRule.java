package com.github.juglee0527.apsengine.scheduling;

import java.util.Comparator;

public class ExplicitPriorityRule implements SchedulingPriorityRule {

    private static final Comparator<SchedulingOrderInput> COMPARATOR =
            Comparator.comparingInt(SchedulingOrderInput::priority)
                    .reversed()
                    .thenComparing(SchedulingOrderInput::dueAt)
                    .thenComparingLong(SchedulingOrderInput::orderId);

    @Override
    public int compare(
            SchedulingOrderInput left,
            SchedulingOrderInput right
    ) {
        return COMPARATOR.compare(left, right);
    }
}
