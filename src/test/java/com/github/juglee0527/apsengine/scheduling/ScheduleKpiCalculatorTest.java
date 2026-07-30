package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;

import org.junit.jupiter.api.Test;

class ScheduleKpiCalculatorTest {

    private static final OffsetDateTime START = OffsetDateTime.of(
            2026, 7, 27, 8, 0, 0, 0,
            ZoneOffset.ofHours(9)
    );

    private final ScheduleKpiCalculator calculator =
            new ScheduleKpiCalculator();

    @Test
    void calculatesTardinessMakespanAndAvailableTimeUtilization() {
        SchedulingOperationInput operation =
                new SchedulingOperationInput(
                        11L,
                        101L,
                        1,
                        "PROCESS",
                        "가공",
                        1,
                        List.of(new WeeklyWorkingTime(
                                DayOfWeek.MONDAY,
                                LocalTime.of(8, 0),
                                LocalTime.of(17, 0)
                        ))
                );
        List<SchedulingOrderInput> orders = List.of(
                order(1L, "PO-LATE", START.plusHours(1), operation),
                order(2L, "PO-ON-TIME", START.plusHours(4), operation)
        );
        SchedulingPlan plan = new SchedulingPlan(
                START,
                START.plusHours(4),
                List.of(
                        task(
                                1L,
                                11L,
                                START.plusMinutes(30),
                                START.plusHours(2),
                                60,
                                30,
                                START.plusHours(1),
                                true
                        ),
                        task(
                                2L,
                                11L,
                                START.plusHours(3),
                                START.plusHours(4),
                                60,
                                0,
                                START.plusHours(4),
                                false
                        )
                )
        );

        ScheduleKpis kpis = calculator.calculate(plan, orders);

        assertThat(kpis.totalTardinessMinutes()).isEqualTo(60);
        assertThat(kpis.delayedOrderCount()).isEqualTo(1);
        assertThat(kpis.makespanMinutes()).isEqualTo(240);
        assertThat(kpis.machineUtilizationPercent())
                .isEqualByComparingTo("62.50");
    }

    private SchedulingOrderInput order(
            long id,
            String number,
            OffsetDateTime dueAt,
            SchedulingOperationInput operation
    ) {
        return new SchedulingOrderInput(
                id,
                number,
                id,
                1,
                START,
                dueAt,
                1,
                List.of(operation)
        );
    }

    private ScheduledTask task(
            long orderId,
            long operationId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            long workingMinutes,
            long changeoverMinutes,
            OffsetDateTime dueAt,
            boolean delayed
    ) {
        return new ScheduledTask(
                orderId,
                "PO-" + orderId,
                operationId,
                101L,
                1,
                "PROCESS",
                "가공",
                changeoverMinutes == 0
                        ? null
                        : startAt.minusMinutes(changeoverMinutes),
                changeoverMinutes,
                startAt,
                endAt,
                workingMinutes,
                dueAt,
                delayed
        );
    }
}
