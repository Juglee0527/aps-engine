package com.github.juglee0527.apsengine.scheduling;

import java.util.Comparator;

@FunctionalInterface
public interface SchedulingPriorityRule
        extends Comparator<SchedulingOrderInput> {
}
