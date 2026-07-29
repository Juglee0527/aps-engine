package com.github.juglee0527.apsengine.capacity;

import java.time.OffsetDateTime;
import java.util.Objects;

public record AvailabilityInterval(
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {

    public AvailabilityInterval {
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "가용시간 종료시각은 시작시각보다 이후여야 합니다."
            );
        }
    }
}
