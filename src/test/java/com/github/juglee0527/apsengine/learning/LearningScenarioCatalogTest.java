package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.Test;

class LearningScenarioCatalogTest {

    private final LearningScenarioCatalog catalog =
            new LearningScenarioCatalog();

    @Test
    void exposesFirstPlanDefinitionCaseInsensitively() {
        LearningScenarioDefinition definition = catalog.get("first_plan");

        assertThat(definition.key()).isEqualTo("FIRST_PLAN");
        assertThat(definition.course()).isEqualTo("A");
        assertThat(definition.expectedOrderCount()).isEqualTo(4);
        assertThat(definition.observationPoints()).contains(
                "오더별 공정 순서",
                "설비별 작업 배치"
        );
    }

    @Test
    void exposesFoundationalAndRuleComparisonLessons() {
        assertThat(catalog.findAll())
                .extracting(LearningScenarioDefinition::key)
                .containsExactly(
                        "FIRST_PLAN",
                        "FINITE_CAPACITY",
                        "PRECEDENCE",
                        "TARDINESS",
                        "RULE_COMPARISON",
                        "CHANGEOVER",
                        "MAINTENANCE",
                        "ALTERNATIVE_MACHINE",
                        "BOTTLENECK",
                        "FROZEN_HORIZON",
                        "MEDIUM_FACTORY",
                        "PERFORMANCE"
                );
        assertThat(catalog.findAll())
                .allSatisfy(definition -> {
                    assertThat(definition.objective()).isNotBlank();
                    assertThat(definition.predictionPrompt()).isNotBlank();
                    assertThat(definition.observationPoints()).isNotEmpty();
                    assertThat(definition.resultExplanation()).isNotBlank();
                    assertThat(definition.nextExperiment()).isNotBlank();
                });
    }

    @Test
    void blueprintsFixCapacityPrecedenceAndTardinessPredictions() {
        LearningScenarioBlueprint finite = catalog.blueprint(
                "FINITE_CAPACITY"
        );
        long finiteMinutes = finite.orders().stream()
                .mapToLong(order -> order.quantity() * 180)
                .sum();
        assertThat(finiteMinutes).isGreaterThan(9 * 60);

        LearningScenarioBlueprint precedence = catalog.blueprint(
                "PRECEDENCE"
        );
        assertThat(precedence.products().getFirst().operations())
                .extracting(
                        LearningScenarioBlueprint.OperationSpec::sequence
                )
                .containsExactly(1, 2, 3);
        assertThat(precedence.products().getFirst().operations())
                .extracting(
                        LearningScenarioBlueprint.OperationSpec::machineCode
                )
                .containsExactly("MAKE", "TEST", "PACK");

        LearningScenarioBlueprint tardiness = catalog.blueprint("TARDINESS");
        assertThat(tardiness.orders().getFirst().dueOffsetMinutes())
                .isLessThan(2 * 180);
    }

    @Test
    void exposesDeterministicMediumAndPerformanceDatasetSizes() {
        LearningScenarioBlueprint medium = catalog.blueprint("MEDIUM_FACTORY");
        LearningScenarioBlueprint performance = catalog.blueprint("PERFORMANCE");

        assertThat(medium.machines()).hasSize(12);
        assertThat(medium.orders()).hasSize(150);
        assertThat(performance.machines()).hasSize(20);
        assertThat(performance.orders()).hasSize(600);
        assertThat(performance.orders())
                .extracting(LearningScenarioBlueprint.OrderSpec::orderNumber)
                .doesNotHaveDuplicates();
    }

    @Test
    void rejectsUnknownScenario() {
        assertThatThrownBy(() -> catalog.get("UNKNOWN"))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.LEARNING_SCENARIO_NOT_FOUND)
                );
    }
}
