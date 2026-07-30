package com.github.juglee0527.apsengine.capacity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BottleneckAnalysis(
        long scheduleRunId,
        OffsetDateTime from,
        OffsetDateTime to,
        BigDecimal thresholdPercent,
        List<BottleneckCandidate> candidates
) {
}
