package com.github.juglee0527.apsengine.constraint.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;

import org.junit.jupiter.api.Test;

class MachineMaintenanceTest {

    private static final OffsetDateTime START =
            OffsetDateTime.parse("2026-08-03T10:00:00+09:00");

    @Test
    void createsMaintenanceWindow() {
        MachineMaintenance maintenance = MachineMaintenance.create(
                machine(),
                START,
                START.plusHours(1),
                "  정기 점검  "
        );

        assertThat(maintenance.startAt()).isEqualTo(START);
        assertThat(maintenance.endAt()).isEqualTo(START.plusHours(1));
        assertThat(maintenance.reason()).isEqualTo("정기 점검");
        assertThat(maintenance.isActive()).isTrue();
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThatThrownBy(() -> MachineMaintenance.create(
                machine(),
                START,
                START,
                "정기 점검"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료시각");
    }

    @Test
    void rejectsBlankReason() {
        assertThatThrownBy(() -> MachineMaintenance.create(
                machine(),
                START,
                START.plusHours(1),
                " "
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사유");
    }

    private Machine machine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, "MACHINE-01", "가공 설비");
    }
}
