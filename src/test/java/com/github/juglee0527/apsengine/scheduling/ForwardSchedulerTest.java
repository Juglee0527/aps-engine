package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;

import org.junit.jupiter.api.Test;

class ForwardSchedulerTest {

    private static final ZoneOffset OFFSET = ZoneOffset.ofHours(9);
    private static final OffsetDateTime MONDAY_EIGHT =
            OffsetDateTime.of(2026, 7, 27, 8, 0, 0, 0, OFFSET);

    private final ForwardScheduler scheduler = new ForwardScheduler();

    @Test
    void schedulesOrdersWithoutMachineOverlap() {
        SchedulingOrderInput first =
                order(1L, "PO-001", 60, 5, operation(11L, 101L, 1, 1));
        SchedulingOrderInput second =
                order(2L, "PO-002", 60, 5, operation(12L, 101L, 1, 1));

        SchedulingPlan plan = scheduler.schedule(
                MONDAY_EIGHT,
                List.of(first, second)
        );

        assertThat(plan.tasks()).hasSize(2);
        assertThat(plan.tasks().get(0).startAt())
                .isEqualTo(MONDAY_EIGHT);
        assertThat(plan.tasks().get(0).endAt())
                .isEqualTo(MONDAY_EIGHT.plusHours(1));
        assertThat(plan.tasks().get(1).startAt())
                .isEqualTo(MONDAY_EIGHT.plusHours(1));
    }

    @Test
    void preservesOperationPrecedenceAcrossMachines() {
        SchedulingOrderInput order = order(
                1L,
                "PO-001",
                60,
                5,
                operation(11L, 101L, 1, 1),
                operation(12L, 102L, 2, 2)
        );

        SchedulingPlan plan =
                scheduler.schedule(MONDAY_EIGHT, List.of(order));

        assertThat(plan.tasks()).hasSize(2);
        assertThat(plan.tasks().get(1).startAt())
                .isEqualTo(plan.tasks().get(0).endAt());
        assertThat(plan.schedulingEnd())
                .isEqualTo(MONDAY_EIGHT.plusHours(3));
    }

    @Test
    void schedulesHigherPriorityOrderFirst() {
        SchedulingOrderInput low =
                order(1L, "PO-LOW", 60, 1, operation(11L, 101L, 1, 1));
        SchedulingOrderInput high =
                order(2L, "PO-HIGH", 60, 10, operation(12L, 101L, 1, 1));

        SchedulingPlan plan =
                scheduler.schedule(MONDAY_EIGHT, List.of(low, high));

        assertThat(plan.tasks().getFirst().orderNumber())
                .isEqualTo("PO-HIGH");
    }

    @Test
    void continuesWorkOnNextWorkingDay() {
        OffsetDateTime fridayFour =
                OffsetDateTime.of(
                        2026, 7, 31, 16, 0, 0, 0, OFFSET
                );
        SchedulingOrderInput order =
                order(1L, "PO-001", 180, 5, operation(11L, 101L, 1, 1));

        SchedulingPlan plan =
                scheduler.schedule(fridayFour, List.of(order));

        assertThat(plan.tasks().getFirst().endAt())
                .isEqualTo(OffsetDateTime.of(
                        2026, 8, 3, 10, 0, 0, 0, OFFSET
                ));
    }

    @Test
    void rejectsOperationWithoutWorkingCalendar() {
        SchedulingOperationInput operation =
                new SchedulingOperationInput(
                        11L,
                        101L,
                        1,
                        "CUT",
                        "절단",
                        1,
                        List.of()
                );
        SchedulingOrderInput order =
                order(1L, "PO-001", 10, 5, operation);

        assertThatThrownBy(() ->
                scheduler.schedule(MONDAY_EIGHT, List.of(order)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("근무시간");
    }

    @Test
    void returnsEmptyPlanForEmptyOrderList() {
        SchedulingPlan plan =
                scheduler.schedule(MONDAY_EIGHT, List.of());

        assertThat(plan.tasks()).isEmpty();
        assertThat(plan.schedulingEnd()).isEqualTo(MONDAY_EIGHT);
    }

    @Test
    void rejectsProcessingTimeOverflow() {
        SchedulingOrderInput order = order(
                1L,
                "PO-001",
                Long.MAX_VALUE,
                5,
                operation(11L, 101L, 1, 2)
        );

        assertThatThrownBy(() ->
                scheduler.schedule(MONDAY_EIGHT, List.of(order)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용 범위");
    }

    private SchedulingOrderInput order(
            long id,
            String orderNumber,
            long quantity,
            int priority,
            SchedulingOperationInput... operations
    ) {
        return new SchedulingOrderInput(
                id,
                orderNumber,
                quantity,
                MONDAY_EIGHT,
                MONDAY_EIGHT.plusDays(5),
                priority,
                List.of(operations)
        );
    }

    private SchedulingOperationInput operation(
            long id,
            long machineId,
            int sequence,
            long minutesPerUnit
    ) {
        return new SchedulingOperationInput(
                id,
                machineId,
                sequence,
                "OP-" + sequence,
                "공정 " + sequence,
                minutesPerUnit,
                weeklyTimes()
        );
    }

    private List<WeeklyWorkingTime> weeklyTimes() {
        return List.of(
                weeklyTime(DayOfWeek.MONDAY),
                weeklyTime(DayOfWeek.TUESDAY),
                weeklyTime(DayOfWeek.WEDNESDAY),
                weeklyTime(DayOfWeek.THURSDAY),
                weeklyTime(DayOfWeek.FRIDAY)
        );
    }

    private WeeklyWorkingTime weeklyTime(DayOfWeek dayOfWeek) {
        return new WeeklyWorkingTime(
                dayOfWeek,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
    }
}
