package com.github.juglee0527.apsengine.scheduling;

public enum DispatchingRule {
    EXPLICIT_PRIORITY,
    EDD,
    SPT;

    SchedulingPriorityRule priorityRule() {
        return switch (this) {
            case EXPLICIT_PRIORITY -> new ExplicitPriorityRule();
            case EDD -> new EarliestDueDateRule();
            case SPT -> new ShortestProcessingTimeRule();
        };
    }
}
