package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PlannedLeadTimeCalculatorTest {

    private static final OffsetDateTime RELEASE_AT =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    private final PlannedLeadTimeCalculator calculator =
            new PlannedLeadTimeCalculator();

    @Test
    void returnsEmptyResultWhenThereIsNoOperation() {
        assertThat(calculator.calculate(List.of())).isEmpty();
    }

    @Test
    void separatesProcessingChangeoverAndWaitingTime() {
        List<LeadTimeOperationInput> inputs = List.of(
                input(RELEASE_AT.plusHours(2), 60, 0),
                input(RELEASE_AT.plusHours(5), 60, 30)
        );

        PlannedLeadTime result =
                calculator.calculate(inputs).getFirst();

        assertThat(result.plannedLeadTimeMinutes()).isEqualTo(300);
        assertThat(result.processingMinutes()).isEqualTo(120);
        assertThat(result.changeoverMinutes()).isEqualTo(30);
        assertThat(result.waitingMinutes()).isEqualTo(150);
        assertThat(result.operationCount()).isEqualTo(2);
    }

    @Test
    void includesDayBoundaryAndHolidayInWaitingTime() {
        OffsetDateTime fridayRelease =
                OffsetDateTime.parse("2026-08-07T16:00:00+09:00");
        OffsetDateTime mondayCompletion =
                OffsetDateTime.parse("2026-08-10T10:00:00+09:00");
        LeadTimeOperationInput input = new LeadTimeOperationInput(
                1L,
                "PO-001",
                10L,
                "PRODUCT-A",
                fridayRelease,
                mondayCompletion,
                120,
                0
        );

        PlannedLeadTime result =
                calculator.calculate(List.of(input)).getFirst();

        assertThat(result.plannedLeadTimeMinutes()).isEqualTo(3_960);
        assertThat(result.processingMinutes()).isEqualTo(120);
        assertThat(result.waitingMinutes()).isEqualTo(3_840);
    }

    @Test
    void rejectsOccupiedMinutesGreaterThanLeadTime() {
        LeadTimeOperationInput invalid =
                input(RELEASE_AT.plusHours(1), 60, 30);

        assertThatThrownBy(() ->
                calculator.calculate(List.of(invalid)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lead Time");
    }

    private LeadTimeOperationInput input(
            OffsetDateTime endAt,
            long processingMinutes,
            long changeoverMinutes
    ) {
        return new LeadTimeOperationInput(
                1L,
                "PO-001",
                10L,
                "PRODUCT-A",
                RELEASE_AT,
                endAt,
                processingMinutes,
                changeoverMinutes
        );
    }
}
