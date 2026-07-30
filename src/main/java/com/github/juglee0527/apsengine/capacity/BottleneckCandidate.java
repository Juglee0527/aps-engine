package com.github.juglee0527.apsengine.capacity;

import java.math.BigDecimal;

public record BottleneckCandidate(
        int rank,
        long machineId,
        String machineCode,
        String machineName,
        long availableMinutes,
        long loadMinutes,
        BigDecimal utilizationPercent,
        boolean capacityExceeded,
        BottleneckReason reason
) {
}
