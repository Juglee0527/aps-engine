package com.github.juglee0527.apsengine.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class BottleneckDetectorTest {

    private static final OffsetDateTime FROM =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
    private static final OffsetDateTime TO = FROM.plusHours(8);

    private final BottleneckDetector detector =
            new BottleneckDetector();

    @Test
    void putsLoadedMachineWithoutAvailableCapacityFirst() {
        BottleneckAnalysis analysis = detector.detect(
                1L,
                FROM,
                TO,
                List.of(
                        input(1, "MACHINE-B", 0, 10),
                        input(2, "MACHINE-A", 100, 90),
                        input(3, "MACHINE-C", 0, 0)
                )
        );

        assertThat(analysis.candidates()).hasSize(2);
        BottleneckCandidate candidate =
                analysis.candidates().getFirst();
        assertThat(candidate.machineId()).isEqualTo(1L);
        assertThat(candidate.utilizationPercent()).isNull();
        assertThat(candidate.capacityExceeded()).isTrue();
        assertThat(candidate.reason())
                .isEqualTo(BottleneckReason.NO_AVAILABLE_CAPACITY);
    }

    @Test
    void sortsSameUtilizationByMachineCodeThenId() {
        BottleneckAnalysis analysis = detector.detect(
                1L,
                FROM,
                TO,
                List.of(
                        input(3, "MACHINE-B", 100, 80),
                        input(2, "MACHINE-A", 200, 160),
                        input(1, "MACHINE-A", 100, 80)
                )
        );

        assertThat(analysis.candidates())
                .extracting(BottleneckCandidate::machineId)
                .containsExactly(1L, 2L, 3L);
        assertThat(analysis.candidates())
                .extracting(BottleneckCandidate::rank)
                .containsExactly(1, 2, 3);
    }

    @Test
    void marksCapacityExceededMachine() {
        BottleneckCandidate candidate = detector.detect(
                1L,
                FROM,
                TO,
                List.of(input(1, "MACHINE-A", 100, 125))
        ).candidates().getFirst();

        assertThat(candidate.utilizationPercent())
                .isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(candidate.capacityExceeded()).isTrue();
        assertThat(candidate.reason())
                .isEqualTo(BottleneckReason.CAPACITY_EXCEEDED);
    }

    @Test
    void excludesMachineBelowThreshold() {
        BottleneckAnalysis analysis = detector.detect(
                1L,
                FROM,
                TO,
                List.of(input(1, "MACHINE-A", 100, 79))
        );

        assertThat(analysis.thresholdPercent())
                .isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(analysis.candidates()).isEmpty();
    }

    private MachineCapacityInput input(
            long machineId,
            String machineCode,
            long availableMinutes,
            long loadMinutes
    ) {
        return new MachineCapacityInput(
                machineId,
                machineCode,
                machineCode + " 이름",
                availableMinutes,
                loadMinutes
        );
    }
}
