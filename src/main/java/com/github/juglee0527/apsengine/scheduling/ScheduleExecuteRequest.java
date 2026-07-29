package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ScheduleExecuteRequest(
        @NotNull UUID executionKey,
        @NotNull OffsetDateTime planningStart
) {
}
