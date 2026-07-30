package com.github.juglee0527.apsengine.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void returnsSameImmutableIntervalsForEmptyUnavailableTime() {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        OffsetDateTime to =
                OffsetDateTime.parse("2026-08-03T17:00:00+09:00");

        List<AvailabilityInterval> withoutUnavailable =
                calculator.intervalsBetween(
                        weekdayTimes(),
                        from,
                        to
                );
        List<AvailabilityInterval> withEmptyUnavailable =
                calculator.intervalsBetween(
                        weekdayTimes(),
                        List.of(),
                        from,
                        to
                );

        assertThat(withEmptyUnavailable)
                .containsExactlyElementsOf(withoutUnavailable);
        assertThatThrownBy(() ->
                withEmptyUnavailable.add(withoutUnavailable.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void excludesMaintenanceFromAvailability() {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        OffsetDateTime to =
                OffsetDateTime.parse("2026-08-03T17:00:00+09:00");
        UnavailableInterval maintenance = new UnavailableInterval(
                OffsetDateTime.parse("2026-08-03T10:00:00+09:00"),
                OffsetDateTime.parse("2026-08-03T11:00:00+09:00")
        );

        List<AvailabilityInterval> intervals =
                calculator.intervalsBetween(
                        weekdayTimes(),
                        List.of(maintenance),
                        from,
                        to
                );

        assertThat(intervals).containsExactly(
                new AvailabilityInterval(
                        from,
                        OffsetDateTime.parse(
                                "2026-08-03T10:00:00+09:00"
                        )
                ),
                new AvailabilityInterval(
                        OffsetDateTime.parse(
                                "2026-08-03T11:00:00+09:00"
                        ),
                        to
                )
        );
        assertThat(calculator.availableMinutes(
                weekdayTimes(),
                List.of(maintenance),
                from,
                to
        )).isEqualTo(480);
    }

    @Test
    void ignoresMaintenanceOutsideWorkingCalendar() {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        OffsetDateTime to =
                OffsetDateTime.parse("2026-08-03T17:00:00+09:00");
        UnavailableInterval maintenance = new UnavailableInterval(
                OffsetDateTime.parse("2026-08-03T18:00:00+09:00"),
                OffsetDateTime.parse("2026-08-03T19:00:00+09:00")
        );

        long minutes = calculator.availableMinutes(
                weekdayTimes(),
                List.of(maintenance),
                from,
                to
        );

        assertThat(minutes).isEqualTo(540);
    }

    @Test
    void treatsTouchingMaintenanceBoundaryAsNonOverlapping() {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        OffsetDateTime to =
                OffsetDateTime.parse("2026-08-03T17:00:00+09:00");
        List<UnavailableInterval> maintenances = List.of(
                new UnavailableInterval(
                        OffsetDateTime.parse(
                                "2026-08-03T07:00:00+09:00"
                        ),
                        from
                ),
                new UnavailableInterval(
                        to,
                        OffsetDateTime.parse(
                                "2026-08-03T18:00:00+09:00"
                        )
                )
        );

        long minutes = calculator.availableMinutes(
                weekdayTimes(),
                maintenances,
                from,
                to
        );

        assertThat(minutes).isEqualTo(540);
    }

    @Test
    void allocatesWorkAroundMaintenance() {
        WorkingAllocation allocation = calculator.allocate(
                weekdayTimes(),
                List.of(new UnavailableInterval(
                        OffsetDateTime.parse(
                                "2026-08-03T10:00:00+09:00"
                        ),
                        OffsetDateTime.parse(
                                "2026-08-03T11:00:00+09:00"
                        )
                )),
                OffsetDateTime.parse("2026-08-03T09:00:00+09:00"),
                120
        );

        assertThat(allocation.startAt()).isEqualTo(
                OffsetDateTime.parse("2026-08-03T09:00:00+09:00")
        );
        assertThat(allocation.endAt()).isEqualTo(
                OffsetDateTime.parse("2026-08-03T12:00:00+09:00")
        );
        assertThat(allocation.workingMinutes()).isEqualTo(120);
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
