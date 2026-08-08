package com.github.juglee0527.apsengine.learning;

import java.util.List;
import java.util.Map;

public record LearningResultCoachResponse(
        String scenarioKey,
        String concept,
        List<String> observationQuestions,
        Map<String, String> kpiMeanings,
        String resultExplanation,
        String nextExperiment
) {
    public LearningResultCoachResponse {
        observationQuestions = List.copyOf(observationQuestions);
        kpiMeanings = Map.copyOf(kpiMeanings);
    }
}
