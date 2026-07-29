package com.github.juglee0527.apsengine.product.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, "MACHINE-01", "가공 설비");
    }
}
