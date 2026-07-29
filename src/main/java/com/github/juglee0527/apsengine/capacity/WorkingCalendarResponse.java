package com.github.juglee0527.apsengine.capacity;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingCalendarResponse(
        Long id,
        Long machineId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {

    public static WorkingCalendarResponse from(WorkingCalendar calendar) {
        return new WorkingCalendarResponse(
                calendar.id(),
                calendar.machine().id(),
                calendar.dayOfWeek(),
                calendar.startTime(),
                calendar.endTime(),
                calendar.isActive()
        );
    }
}
