package com.github.juglee0527.apsengine.learning;

import java.util.UUID;

import com.github.juglee0527.apsengine.scheduling.DispatchingRule;

import jakarta.validation.constraints.NotNull;

public record LearningScheduleRequest(
        @NotNull UUID executionKey,
        DispatchingRule dispatchingRule
) {
    public LearningScheduleRequest {
        dispatchingRule = dispatchingRule == null
                ? DispatchingRule.EXPLICIT_PRIORITY
                : dispatchingRule;
    }
}
