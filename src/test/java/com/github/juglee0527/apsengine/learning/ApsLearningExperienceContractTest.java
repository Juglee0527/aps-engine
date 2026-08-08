package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ApsLearningExperienceContractTest {

    private final LearningScenarioCatalog catalog =
            new LearningScenarioCatalog();
    private final LearningResultCoach coach =
            new LearningResultCoach(catalog);

    @Test
    void everyCourseHasExecutableScenariosAndCompleteCoaching() {
        assertThat(catalog.findAll())
                .extracting(LearningScenarioDefinition::course)
                .contains("A", "B", "C", "D", "E", "F");

        for (LearningScenarioDefinition scenario : catalog.findAll()) {
            LearningScenarioBlueprint blueprint = catalog.blueprint(
                    scenario.key()
            );
            LearningResultCoachResponse resultCoach = coach.get(
                    scenario.key()
            );

            assertThat(blueprint.machines()).hasSize(
                    scenario.expectedMachineCount()
            );
            assertThat(blueprint.products()).hasSize(
                    scenario.expectedProductCount()
            );
            assertThat(blueprint.orders()).hasSize(
                    scenario.expectedOrderCount()
            );
            assertThat(blueprint.orders())
                    .extracting(LearningScenarioBlueprint.OrderSpec::orderNumber)
                    .doesNotHaveDuplicates();
            assertThat(resultCoach.observationQuestions())
                    .hasSameSizeAs(scenario.observationPoints());
            assertThat(resultCoach.kpiMeanings())
                    .containsKeys("MAKESPAN", "TASK_COUNT");
            assertThat(resultCoach.resultExplanation()).isNotBlank();
            assertThat(resultCoach.nextExperiment()).isNotBlank();
        }
    }

    @Test
    void catalogKeepsLearningKeysAndScaleBoundariesStable() {
        assertThat(catalog.findAll()).hasSize(12);
        assertThat(catalog.findAll())
                .extracting(LearningScenarioDefinition::key)
                .containsAll(Set.of(
                        "FIRST_PLAN",
                        "RULE_COMPARISON",
                        "CHANGEOVER",
                        "MAINTENANCE",
                        "ALTERNATIVE_MACHINE",
                        "BOTTLENECK",
                        "FROZEN_HORIZON",
                        "MEDIUM_FACTORY",
                        "PERFORMANCE"
                ));
        assertThat(catalog.get("MEDIUM_FACTORY").expectedOrderCount())
                .isEqualTo(150);
        assertThat(catalog.get("PERFORMANCE").expectedOrderCount())
                .isEqualTo(600);
    }
}
