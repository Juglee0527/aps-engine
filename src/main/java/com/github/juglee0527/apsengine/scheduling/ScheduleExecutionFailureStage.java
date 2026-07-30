package com.github.juglee0527.apsengine.scheduling;

enum ScheduleExecutionFailureStage {

    QUEUE("queue"),
    START("start"),
    CALCULATION("calculation"),
    RESULT_LINK("result_link");

    private final String metricTag;

    ScheduleExecutionFailureStage(String metricTag) {
        this.metricTag = metricTag;
    }

    String metricTag() {
        return metricTag;
    }
}
