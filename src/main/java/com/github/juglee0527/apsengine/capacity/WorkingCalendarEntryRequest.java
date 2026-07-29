package com.github.juglee0527.apsengine.capacity;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record WorkingCalendarEntryRequest(
        @NotNull(message = "요일은 필수입니다.")
        DayOfWeek dayOfWeek,

        @NotNull(message = "근무 시작시각은 필수입니다.")
        LocalTime startTime,

        @NotNull(message = "근무 종료시각은 필수입니다.")
        LocalTime endTime
) {
}
