package com.github.juglee0527.apsengine.learning;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record LearningScenarioCreateRequest(
        @NotNull UUID requestKey
) {
}
