package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ScheduleTaskSearchParameters(
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        @Min(1) Long machineId,
        OffsetDateTime from,
        OffsetDateTime to,
        @Size(max = 100) String query
) {
    public ScheduleTaskSearchParameters {
        page = page == null ? 0 : page;
        size = size == null ? 100 : size;
        query = query == null || query.isBlank()
                ? null
                : query.trim().toLowerCase();
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException(
                    "간트 종료시각은 시작시각보다 이후여야 합니다."
            );
        }
    }
}
