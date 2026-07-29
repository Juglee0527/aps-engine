package com.github.juglee0527.apsengine.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;

import org.junit.jupiter.api.Test;

class ProductionOrderTest {

    private static final OffsetDateTime RELEASE_AT =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
    private static final OffsetDateTime DUE_AT =
            OffsetDateTime.parse("2026-08-04T18:00:00+09:00");

    @Test
    void createsDraftOrderAndConfirmsIt() {
        ProductionOrder order = order();

        assertThat(order.orderNumber()).isEqualTo("PO-2026-001");
        assertThat(order.status()).isEqualTo(ProductionOrderStatus.DRAFT);

        order.confirm();

        assertThat(order.status())
                .isEqualTo(ProductionOrderStatus.CONFIRMED);
    }

    @Test
    void rejectsInvalidDueAt() {
        assertThatThrownBy(() -> ProductionOrder.create(
                routing(),
                "PO-2026-001",
                10,
                RELEASE_AT,
                RELEASE_AT,
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("납기시각");
    }

    @Test
    void rejectsConfirmingTwice() {
        ProductionOrder order = order();
        order.confirm();

        assertThatThrownBy(order::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMED");
    }

    private ProductionOrder order() {
        return ProductionOrder.create(
                routing(),
                "po-2026-001",
                10,
                RELEASE_AT,
                DUE_AT,
                50
        );
    }

    private Routing routing() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        Routing routing =
                Routing.create(product, "ROUTING-01", "표준 Routing");
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        Machine machine = Machine.create(line, "MACHINE-01", "가공 설비");
        routing.addOperation(10, "CUT", "절단", 15, machine);
        return routing;
    }
}
