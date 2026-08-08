package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.LongStream;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ScheduleExecutionTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse("2026-07-30T18:00:00+09:00");

    @Test
    void movesFromQueuedToRunningAndCompleted() {
        UUID executionKey = UUID.randomUUID();
        ScheduleExecution execution = ScheduleExecution.queue(
                executionKey,
                NOW,
                DispatchingRule.EDD,
                NOW
        );
        ScheduleRun result = ScheduleRun.create(
                executionKey,
                new SchedulingPlan(NOW, NOW, List.of()),
                NOW,
                DispatchingRule.EDD,
                ScheduleKpis.empty()
        );
        ReflectionTestUtils.setField(result, "id", 20L);

        execution.start(NOW.plusSeconds(1));
        execution.complete(result, NOW.plusSeconds(2));

        assertThat(execution.status())
                .isEqualTo(ScheduleExecutionStatus.COMPLETED);
        assertThat(execution.resultScheduleRunId()).isEqualTo(20L);
        assertThat(execution.startedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(execution.completedAt())
                .isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    void failsQueuedExecutionWithoutStartedTimestamp() {
        ScheduleExecution execution = ScheduleExecution.queue(
                UUID.randomUUID(),
                NOW,
                DispatchingRule.EXPLICIT_PRIORITY,
                NOW
        );

        execution.fail("대기열이 가득 찼습니다.", NOW.plusSeconds(1));

        assertThat(execution.status())
                .isEqualTo(ScheduleExecutionStatus.FAILED);
        assertThat(execution.startedAt()).isNull();
        assertThat(execution.failureReason()).contains("대기열");
    }

    @Test
    void rejectsResultWithDifferentExecutionKey() {
        ScheduleExecution execution = ScheduleExecution.queue(
                UUID.randomUUID(),
                NOW,
                DispatchingRule.SPT,
                NOW
        );
        execution.start(NOW);
        ScheduleRun result = ScheduleRun.create(
                UUID.randomUUID(),
                new SchedulingPlan(NOW, NOW, List.of()),
                NOW
        );

        assertThatThrownBy(() -> execution.complete(result, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("키");
    }

    @Test
    void comparesEveryIdempotencyInput() {
        UUID executionKey = UUID.randomUUID();
        ScheduleExecution execution = ScheduleExecution.queue(
                executionKey,
                NOW,
                DispatchingRule.EDD,
                NOW
        );

        assertThat(execution.matches(
                NOW,
                DispatchingRule.EDD,
                null,
                null
        )).isTrue();
        assertThat(execution.matches(
                NOW.plusMinutes(1),
                DispatchingRule.EDD,
                null,
                null
        )).isFalse();
        assertThat(execution.matches(
                NOW,
                DispatchingRule.SPT,
                null,
                null
        )).isFalse();
    }

    @Test
    void normalizesDuplicateIdsAndComparesPlanningScope() {
        ScheduleExecution execution = ScheduleExecution.queue(
                UUID.randomUUID(),
                NOW,
                DispatchingRule.EDD,
                List.of(8L, 3L, 8L),
                NOW
        );

        assertThat(execution.productionOrderIds())
                .containsExactly(3L, 8L);
        assertThat(execution.matches(
                NOW,
                DispatchingRule.EDD,
                null,
                null,
                List.of(3L, 8L)
        )).isTrue();
        assertThat(execution.matches(
                NOW,
                DispatchingRule.EDD,
                null,
                null,
                List.of(3L, 9L)
        )).isFalse();
    }

    @Test
    void retainsLargePlanningScope() {
        List<Long> ids = LongStream.rangeClosed(1, 1_000)
                .boxed()
                .toList();

        ScheduleExecution execution = ScheduleExecution.queue(
                UUID.randomUUID(),
                NOW,
                DispatchingRule.SPT,
                ids,
                NOW
        );

        assertThat(execution.productionOrderIds())
                .hasSize(1_000)
                .containsExactlyElementsOf(ids);
    }
}
