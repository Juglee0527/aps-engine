package com.github.juglee0527.apsengine.learning;

public record LearningScenarioDefinition(
        String key,
        String course,
        String title,
        String description,
        int expectedMachineCount,
        int expectedProductCount,
        int expectedOrderCount
) {
}
