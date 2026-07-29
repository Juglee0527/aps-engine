package com.github.juglee0527.apsengine.capacity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

public record WeeklyWorkingTime(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {

    public WeeklyWorkingTime {
        Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "근무 종료시각은 시작시각보다 이후여야 합니다."
            );
        }
    }
}
