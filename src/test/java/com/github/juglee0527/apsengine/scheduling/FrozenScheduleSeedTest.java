package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;

import org.junit.jupiter.api.Test;

class FrozenScheduleSeedTest {

    private static final OffsetDateTime START = OffsetDateTime.of(
            2026, 7, 27, 8, 0, 0, 0,
            ZoneOffset.ofHours(9)
    );

    @Test
    void keepsFrozenTaskAndSeedsMachineAvailabilityAndProduct() {
        ScheduledTask frozenTask = new ScheduledTask(
                1L,
                "PO-FROZEN",
                11L,
                101L,
                1,
                "PROCESS-A",
                "A 가공",
                null,
                0,
                START,
                START.plusHours(2),
                120,
                START.plusDays(1),
                false
        );
        FrozenScheduleSeed seed = new FrozenScheduleSeed(
                List.of(frozenTask),
                Map.of(101L, frozenTask.endAt()),
                Map.of(101L, 201L),
                Map.of(2L, START.plusHours(1))
        );
        SchedulingOrderInput futureOrder = new SchedulingOrderInput(
                2L,
                "PO-FUTURE",
                202L,
                1,
                START.plusHours(1),
                START.plusDays(1),
                50,
                List.of(new SchedulingOperationInput(
                        12L,
                        101L,
                        1,
                        "PROCESS-B",
                        "B 가공",
                        60,
                        weekdayWorkingTimes()
                ))
        );

        SchedulingPlan plan = new ForwardScheduler().schedule(
                START,
                List.of(futureOrder),
                List.of(new SchedulingChangeoverInput(
                        101L,
                        201L,
                        202L,
                        30
                )),
                seed
        );

        assertThat(plan.tasks()).hasSize(2);
        assertThat(plan.tasks().getFirst()).isSameAs(frozenTask);
        ScheduledTask rescheduled = plan.tasks().get(1);
        assertThat(rescheduled.changeoverStartAt())
                .isEqualTo(START.plusHours(2));
        assertThat(rescheduled.startAt())
                .isEqualTo(START.plusHours(2).plusMinutes(30));
        assertThat(rescheduled.endAt())
                .isEqualTo(START.plusHours(3).plusMinutes(30));
    }

    private List<WeeklyWorkingTime> weekdayWorkingTimes() {
        return List.of(new WeeklyWorkingTime(
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        ));
    }
}
