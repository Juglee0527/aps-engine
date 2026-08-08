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
        assertThat(definition.expectedOrderCount()).isEqualTo(8);
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
