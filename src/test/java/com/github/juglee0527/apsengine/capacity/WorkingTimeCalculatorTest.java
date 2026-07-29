package com.github.juglee0527.apsengine.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class WorkingTimeCalculatorTest {

    private final WorkingTimeCalculator calculator =
            new WorkingTimeCalculator();

    @Test
    void calculatesAvailabilityAcrossTwoWorkingDays() {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T07:00:00+09:00");
        OffsetDateTime to =
                OffsetDateTime.parse("2026-08-04T10:00:00+09:00");

        long minutes = calculator.availableMinutes(
                weekdayTimes(),
                from,
                to
        );

        assertThat(minutes).isEqualTo(660);
    }

    @Test
    void allocatesWorkAcrossWeekend() {
        WorkingAllocation allocation = calculator.allocate(
                weekdayTimes(),
                OffsetDateTime.parse("2026-08-07T16:00:00+09:00"),
                180
        );

        assertThat(allocation.startAt()).isEqualTo(
                OffsetDateTime.parse("2026-08-07T16:00:00+09:00")
        );
        assertThat(allocation.endAt()).isEqualTo(
                OffsetDateTime.parse("2026-08-10T10:00:00+09:00")
        );
        assertThat(allocation.workingMinutes()).isEqualTo(180);
    }

    @Test
    void clipsAvailabilityToRequestedRange() {
        List<AvailabilityInterval> intervals =
                calculator.intervalsBetween(
                        weekdayTimes(),
                        OffsetDateTime.parse(
                                "2026-08-03T12:00:00+09:00"
                        ),
                        OffsetDateTime.parse(
                                "2026-08-03T14:00:00+09:00"
                        )
                );

        assertThat(intervals).containsExactly(new AvailabilityInterval(
                OffsetDateTime.parse("2026-08-03T12:00:00+09:00"),
                OffsetDateTime.parse("2026-08-03T14:00:00+09:00")
        ));
    }

    private List<WeeklyWorkingTime> weekdayTimes() {
        return List.of(
                workingTime(DayOfWeek.MONDAY),
                workingTime(DayOfWeek.TUESDAY),
                workingTime(DayOfWeek.WEDNESDAY),
                workingTime(DayOfWeek.THURSDAY),
                workingTime(DayOfWeek.FRIDAY)
        );
    }

    private WeeklyWorkingTime workingTime(DayOfWeek dayOfWeek) {
        return new WeeklyWorkingTime(
                dayOfWeek,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
    }
}
