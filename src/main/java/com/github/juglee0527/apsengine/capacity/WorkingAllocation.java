package com.github.juglee0527.apsengine.capacity;

import java.time.OffsetDateTime;

public record WorkingAllocation(
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long workingMinutes
) {
}
