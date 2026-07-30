package com.github.juglee0527.apsengine.scheduling;

import java.math.BigDecimal;
import java.util.Objects;

public record ScheduleKpis(
        long totalTardinessMinutes,
        int delayedOrderCount,
        long makespanMinutes,
        BigDecimal machineUtilizationPercent
) {

    public ScheduleKpis {
        if (totalTardinessMinutes < 0
                || delayedOrderCount < 0
                || makespanMinutes < 0) {
            throw new IllegalArgumentException(
                    "스케줄 KPI는 음수일 수 없습니다."
            );
        }
        machineUtilizationPercent = Objects.requireNonNull(
                machineUtilizationPercent,
                "machineUtilizationPercent must not be null"
        );
        if (machineUtilizationPercent.signum() < 0
                || machineUtilizationPercent
                .compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "설비 가동률은 0% 이상 100% 이하여야 합니다."
            );
        }
    }

    public static ScheduleKpis empty() {
        return new ScheduleKpis(0, 0, 0, BigDecimal.ZERO);
    }
}
