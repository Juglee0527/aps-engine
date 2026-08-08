package com.github.juglee0527.apsengine.learning;

import java.util.UUID;

import com.github.juglee0527.apsengine.scheduling.DispatchingRule;

import jakarta.validation.constraints.NotNull;

public record FrozenHorizonLabRequest(
        @NotNull UUID baselineExecutionKey,
        @NotNull UUID rescheduleExecutionKey,
        DispatchingRule dispatchingRule
) {
}
