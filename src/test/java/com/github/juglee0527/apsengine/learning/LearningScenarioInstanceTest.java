package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LearningScenarioInstanceTest {

    @Test
    void createsStableNamespaceAndResetsIdempotently() {
        UUID requestKey = UUID.fromString(
                "12345678-1234-5678-90ab-1234567890ab"
        );
        OffsetDateTime now = OffsetDateTime.parse(
                "2026-08-08T10:00:00+09:00"
        );

        LearningScenarioInstance instance = LearningScenarioInstance.create(
                requestKey,
                "FIRST_PLAN",
                now.plusDays(2),
                now
        );

        assertThat(instance.namespace()).isEqualTo("LEARN-123456781234");
        assertThat(instance.status()).isEqualTo(LearningScenarioStatus.READY);

        instance.reset();
        instance.reset();

        assertThat(instance.status()).isEqualTo(LearningScenarioStatus.RESET);
    }

    @Test
    void rejectsMissingRequiredValues() {
        assertThatThrownBy(() -> LearningScenarioInstance.create(
                null,
                "FIRST_PLAN",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
