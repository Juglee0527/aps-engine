package com.github.juglee0527.apsengine.learning;

import java.time.OffsetDateTime;
import java.util.List;

import com.github.juglee0527.apsengine.scheduling.ScheduleRunResponse;

public record FrozenHorizonLabResponse(
        OffsetDateTime frozenAt,
        OffsetDateTime maintenanceStartAt,
        OffsetDateTime maintenanceEndAt,
        ScheduleRunResponse baseline,
        ScheduleRunResponse rescheduled,
        List<FrozenHorizonTaskChange> changes,
        String explanation
) {
}
