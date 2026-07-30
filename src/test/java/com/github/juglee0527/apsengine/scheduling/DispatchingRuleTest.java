package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;

import org.junit.jupiter.api.Test;

class DispatchingRuleTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.of(
                    2026, 7, 27, 8, 0, 0, 0,
                    ZoneOffset.ofHours(9)
            );

    @Test
    void producesDifferentFirstOrderForEachRule() {
        SchedulingOrderInput explicitFirst =
                order(1L, "EXPLICIT", 120, 100, 3);
        SchedulingOrderInput shortestFirst =
                order(2L, "SHORTEST", 30, 10, 2);
        SchedulingOrderInput earliestDueFirst =
                order(3L, "EARLIEST-DUE", 60, 50, 1);
        List<SchedulingOrderInput> orders = List.of(
                shortestFirst,
                earliestDueFirst,
                explicitFirst
        );

        assertThat(firstOrderId(DispatchingRule.EXPLICIT_PRIORITY, orders))
                .isEqualTo(1L);
        assertThat(firstOrderId(DispatchingRule.EDD, orders))
                .isEqualTo(3L);
        assertThat(firstOrderId(DispatchingRule.SPT, orders))
                .isEqualTo(2L);
    }

    @Test
    void resolvesEddAndSptTiesDeterministically() {
        SchedulingOrderInput lowerId =
                order(1L, "LOWER-ID", 60, 50, 1);
        SchedulingOrderInput higherId =
                order(2L, "HIGHER-ID", 60, 50, 1);
        List<SchedulingOrderInput> reversed =
                List.of(higherId, lowerId);

        assertThat(firstOrderId(DispatchingRule.EDD, reversed))
                .isEqualTo(1L);
        assertThat(firstOrderId(DispatchingRule.SPT, reversed))
                .isEqualTo(1L);
    }

    private long firstOrderId(
            DispatchingRule rule,
            List<SchedulingOrderInput> orders
    ) {
        SchedulingPlan plan = new ForwardScheduler(rule.priorityRule())
                .schedule(PLANNING_START, orders);
        return plan.tasks().getFirst().orderId();
    }

    private SchedulingOrderInput order(
            long id,
            String number,
            long processingMinutes,
            int priority,
            long dueDays
    ) {
        return new SchedulingOrderInput(
                id,
                number,
                id,
                1,
                PLANNING_START,
                PLANNING_START.plusDays(dueDays),
                priority,
                List.of(new SchedulingOperationInput(
                        id,
                        100L,
                        1,
                        "PROCESS",
                        "가공",
                        processingMinutes,
                        workingTimes()
                ))
        );
    }

    private List<WeeklyWorkingTime> workingTimes() {
        return List.of(new WeeklyWorkingTime(
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        ));
    }
}
