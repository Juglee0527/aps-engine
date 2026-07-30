package com.github.juglee0527.apsengine.product.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;

import org.junit.jupiter.api.Test;

class RoutingTest {

    @Test
    void createsOrderedOperations() {
        Routing routing = routing();
        Machine machine = machine();

        routing.addOperation(10, "cut", "절단", 15, machine);
        routing.addOperation(20, "assembly", "조립", 30, machine);

        assertThat(routing.code()).isEqualTo("ROUTING-01");
        assertThat(routing.operations())
                .extracting(Operation::sequence)
                .containsExactly(10, 20);
        assertThat(routing.operations().getFirst().code())
                .isEqualTo("CUT");
        assertThat(routing.operations().getFirst().machineCandidates())
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.machine()).isSameAs(machine);
                    assertThat(candidate.priority()).isEqualTo(1);
                });
    }

    @Test
    void allowsAlternativeMachinesWithSamePriority() {
        Routing routing = routing();
        Machine primaryMachine = machine("MACHINE-01");
        Machine alternativeMachine = machine("MACHINE-02");

        routing.addOperation(
                10,
                "CUT",
                "절단",
                15,
                primaryMachine,
                Map.of(primaryMachine, 1, alternativeMachine, 1)
        );

        assertThat(routing.operations().getFirst().machineCandidates())
                .hasSize(2)
                .extracting(
                        candidate -> candidate.machine().code(),
                        OperationMachineCandidate::priority
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("MACHINE-01", 1),
                        org.assertj.core.groups.Tuple.tuple("MACHINE-02", 1)
                );
    }

    @Test
    void rejectsOperationWithoutMachineCandidate() {
        Machine primaryMachine = machine();

        assertThatThrownBy(() -> routing().addOperation(
                10,
                "CUT",
                "절단",
                15,
                primaryMachine,
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("후보 설비가 하나 이상");
    }

    @Test
    void rejectsCandidatesWithoutPrimaryMachine() {
        Machine primaryMachine = machine("MACHINE-01");
        Machine alternativeMachine = machine("MACHINE-02");

        assertThatThrownBy(() -> routing().addOperation(
                10,
                "CUT",
                "절단",
                15,
                primaryMachine,
                Map.of(alternativeMachine, 1)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주 설비가 포함");
    }

    @Test
    void rejectsDuplicatedOperationSequence() {
        Routing routing = routing();
        Machine machine = machine();
        routing.addOperation(10, "CUT", "절단", 15, machine);

        assertThatThrownBy(() -> routing.addOperation(
                10,
                "ASSEMBLY",
                "조립",
                30,
                machine
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("순서는 중복");
    }

    @Test
    void rejectsInvalidProcessingTime() {
        assertThatThrownBy(() -> routing().addOperation(
                10,
                "CUT",
                "절단",
                0,
                machine()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가공시간");
    }

    private Routing routing() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        return Routing.create(product, "routing-01", "표준 Routing");
    }

    private Machine machine() {
        return machine("MACHINE-01");
    }

    private Machine machine(String code) {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, code, "가공 설비");
    }
}
