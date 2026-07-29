package com.github.juglee0527.apsengine.machine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;

import org.junit.jupiter.api.Test;

class MachineTest {

    @Test
    void createsAvailableMachineWithNormalizedValues() {
        ProductionLine productionLine = productionLine();

        Machine machine = Machine.create(
                productionLine,
                "  machine-01  ",
                "  절단 설비  "
        );

        assertThat(machine.id()).isNull();
        assertThat(machine.productionLine()).isSameAs(productionLine);
        assertThat(machine.code()).isEqualTo("MACHINE-01");
        assertThat(machine.name()).isEqualTo("절단 설비");
        assertThat(machine.status()).isEqualTo(MachineStatus.AVAILABLE);
    }

    @Test
    void stopsAndRestartsMachine() {
        Machine machine =
                Machine.create(productionLine(), "MACHINE-01", "절단 설비");

        machine.stop();
        assertThat(machine.status()).isEqualTo(MachineStatus.STOPPED);

        machine.restart();
        assertThat(machine.status()).isEqualTo(MachineStatus.AVAILABLE);
    }

    @Test
    void deactivatesAndReactivatesMachine() {
        Machine machine =
                Machine.create(productionLine(), "MACHINE-01", "절단 설비");

        machine.deactivate();
        assertThat(machine.status()).isEqualTo(MachineStatus.INACTIVE);

        machine.reactivate();
        assertThat(machine.status()).isEqualTo(MachineStatus.AVAILABLE);
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Machine machine =
                Machine.create(productionLine(), "MACHINE-01", "절단 설비");

        assertThatThrownBy(machine::restart)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AVAILABLE")
                .hasMessageContaining("AVAILABLE");

        machine.deactivate();
        assertThatThrownBy(machine::deactivate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidMachineValues() {
        ProductionLine productionLine = productionLine();

        assertThatThrownBy(() ->
                Machine.create(productionLine, "machine 01", "절단 설비"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                Machine.create(productionLine, "MACHINE-01", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ProductionLine productionLine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        return ProductionLine.create(factory, "LINE-01", "조립 라인");
    }
}

