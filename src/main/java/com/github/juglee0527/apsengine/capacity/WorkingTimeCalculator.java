package com.github.juglee0527.apsengine.capacity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorkingTimeCalculator {

    private static final int MAX_SEARCH_DAYS = 3_660;

    public List<AvailabilityInterval> intervalsBetween(
            List<WeeklyWorkingTime> weeklyTimes,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        validateRange(weeklyTimes, from, to);
        List<AvailabilityInterval> intervals = new ArrayList<>();
        ZoneOffset offset = from.getOffset();
        LocalDate date = from.toLocalDate();
        LocalDate lastDate = to.toLocalDate();

        while (!date.isAfter(lastDate)) {
            appendIntervalsForDate(
                    weeklyTimes,
                    date,
                    offset,
                    from,
                    to,
                    intervals
            );
            date = date.plusDays(1);
        }
        intervals.sort(Comparator.comparing(
                AvailabilityInterval::startAt
        ));
        return List.copyOf(intervals);
    }

    public long availableMinutes(
            List<WeeklyWorkingTime> weeklyTimes,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        long totalMinutes = 0;
        for (AvailabilityInterval interval : intervalsBetween(
                weeklyTimes,
                from,
                to
        )) {
            totalMinutes = Math.addExact(
                    totalMinutes,
                    Duration.between(
                            interval.startAt(),
                            interval.endAt()
                    ).toMinutes()
            );
        }
        return totalMinutes;
    }

    public WorkingAllocation allocate(
            List<WeeklyWorkingTime> weeklyTimes,
            OffsetDateTime earliestStart,
            long requiredWorkingMinutes
    ) {
        if (weeklyTimes == null || weeklyTimes.isEmpty()) {
            throw new IllegalArgumentException(
                    "근무시간이 하나 이상 필요합니다."
            );
        }
        if (requiredWorkingMinutes < 1) {
            throw new IllegalArgumentException(
                    "필요 작업시간은 1분 이상이어야 합니다."
            );
        }

        OffsetDateTime cursor = earliestStart;
        OffsetDateTime allocationStart = null;
        long remainingMinutes = requiredWorkingMinutes;

        for (int dayCount = 0;
             dayCount < MAX_SEARCH_DAYS;
             dayCount++) {
            OffsetDateTime dayEnd = cursor.toLocalDate()
                    .plusDays(1)
                    .atStartOfDay()
                    .atOffset(cursor.getOffset());
            List<AvailabilityInterval> intervals = intervalsBetween(
                    weeklyTimes,
                    cursor,
                    dayEnd
            );

            for (AvailabilityInterval interval : intervals) {
                if (allocationStart == null) {
                    allocationStart = interval.startAt();
                }
                long intervalMinutes = Duration.between(
                        interval.startAt(),
                        interval.endAt()
                ).toMinutes();
                if (remainingMinutes <= intervalMinutes) {
                    return new WorkingAllocation(
                            allocationStart,
                            interval.startAt().plusMinutes(remainingMinutes),
                            requiredWorkingMinutes
                    );
                }
                remainingMinutes -= intervalMinutes;
            }
            cursor = dayEnd;
        }

        throw new IllegalStateException(
                "10년 이내에 필요한 작업시간을 배정할 수 없습니다."
        );
    }

    private void appendIntervalsForDate(
            List<WeeklyWorkingTime> weeklyTimes,
            LocalDate date,
            ZoneOffset offset,
            OffsetDateTime from,
            OffsetDateTime to,
            List<AvailabilityInterval> target
    ) {
        for (WeeklyWorkingTime weeklyTime : weeklyTimes) {
            if (weeklyTime.dayOfWeek() != date.getDayOfWeek()) {
                continue;
            }
            OffsetDateTime start =
                    date.atTime(weeklyTime.startTime()).atOffset(offset);
            OffsetDateTime end =
                    date.atTime(weeklyTime.endTime()).atOffset(offset);
            OffsetDateTime clippedStart =
                    start.isBefore(from) ? from : start;
            OffsetDateTime clippedEnd = end.isAfter(to) ? to : end;
            if (clippedEnd.isAfter(clippedStart)) {
                target.add(new AvailabilityInterval(
                        clippedStart,
                        clippedEnd
                ));
            }
        }
    }

    private void validateRange(
            List<WeeklyWorkingTime> weeklyTimes,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (weeklyTimes == null) {
            throw new IllegalArgumentException(
                    "근무시간 목록은 null일 수 없습니다."
            );
        }
        if (from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException(
                    "조회 종료시각은 시작시각보다 이후여야 합니다."
            );
        }
    }
}
