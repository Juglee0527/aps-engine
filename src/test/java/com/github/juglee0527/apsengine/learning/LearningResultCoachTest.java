package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LearningResultCoachTest {

    private final LearningResultCoach coach = new LearningResultCoach(
            new LearningScenarioCatalog()
    );

    @Test
    void mapsTardinessLessonToQuestionsAndKpiMeanings() {
        LearningResultCoachResponse result = coach.get("TARDINESS");

        assertThat(result.observationQuestions())
                .hasSize(3)
                .allMatch(question -> question.endsWith("찾았나요?"));
        assertThat(result.kpiMeanings()).containsKeys(
                "MAKESPAN",
                "TOTAL_TARDINESS",
                "DELAYED_ORDERS"
        );
        assertThat(result.resultExplanation()).contains("CAPA");
        assertThat(result.nextExperiment()).contains("EDD");
    }

    @Test
    void mapsFrozenHorizonLessonWithoutInventingUtilizationCoach() {
        LearningResultCoachResponse result = coach.get("FROZEN_HORIZON");

        assertThat(result.observationQuestions())
                .anyMatch(question -> question.contains("고정 작업"));
        assertThat(result.kpiMeanings()).doesNotContainKey("UTILIZATION");
        assertThat(result.resultExplanation()).contains("경계");
        assertThat(result.nextExperiment()).contains("동결 기준");
    }
}
