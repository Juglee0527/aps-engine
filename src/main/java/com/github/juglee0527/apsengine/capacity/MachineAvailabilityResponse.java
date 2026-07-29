package com.github.juglee0527.apsengine.capacity;

import java.time.OffsetDateTime;
import java.util.List;

public record MachineAvailabilityResponse(
        Long machineId,
        OffsetDateTime from,
        OffsetDateTime to,
        long availableMinutes,
        List<AvailabilityInterval> intervals
) {

    public MachineAvailabilityResponse {
        intervals = List.copyOf(intervals);
    }
}
