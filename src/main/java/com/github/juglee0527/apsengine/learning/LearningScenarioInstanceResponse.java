package com.github.juglee0527.apsengine.learning;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LearningScenarioInstanceResponse(
        long id,
        UUID requestKey,
        String scenarioKey,
        String namespace,
        LearningScenarioStatus status,
        OffsetDateTime planningStart,
        OffsetDateTime createdAt,
        long trackedEntityCount
) {
    static LearningScenarioInstanceResponse from(
            LearningScenarioInstance instance,
            long trackedEntityCount
    ) {
        return new LearningScenarioInstanceResponse(
                instance.id(),
                instance.requestKey(),
                instance.scenarioKey(),
                instance.namespace(),
                instance.status(),
                instance.planningStart(),
                instance.createdAt(),
                trackedEntityCount
        );
    }
}
