package com.github.juglee0527.apsengine.learning;

import java.util.List;

public record LearningScenarioDefinition(
        String key,
        String course,
        String title,
        String description,
        int expectedMachineCount,
        int expectedProductCount,
        int expectedOrderCount,
        String objective,
        String predictionPrompt,
        List<String> observationPoints,
        String resultExplanation,
        String nextExperiment
) {
    public LearningScenarioDefinition(
            String key,
            String course,
            String title,
            String description,
            int expectedMachineCount,
            int expectedProductCount,
            int expectedOrderCount
    ) {
        this(
                key,
                course,
                title,
                description,
                expectedMachineCount,
                expectedProductCount,
                expectedOrderCount,
                "",
                "",
                List.of(),
                "",
                ""
        );
    }

    public LearningScenarioDefinition {
        observationPoints = List.copyOf(observationPoints);
    }
}
