package com.github.juglee0527.apsengine.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("local")
@SpringBootTest
@Transactional
@Rollback
@EnabledIfEnvironmentVariable(
        named = "APS_POSTGRES_INTEGRATION_TEST",
        matches = "true"
)
class ProductionOrderJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductionOrderRepository productionOrderRepository;

    @Test
    void persistsAndLoadsOrderForResponse() {
        Product product =
                Product.create("PRODUCT-PO-MAP", "매핑 품목", ProductUnit.PIECE);
        entityManager.persist(product);
        Factory factory = Factory.create("FACTORY-PO-MAP", "매핑 공장");
        entityManager.persist(factory);
        ProductionLine line =
                ProductionLine.create(factory, "LINE-PO", "매핑 라인");
        entityManager.persist(line);
        Machine machine = Machine.create(line, "MACHINE-PO", "매핑 설비");
        entityManager.persist(machine);
        Routing routing =
                Routing.create(product, "ROUTING-PO", "매핑 Routing");
        routing.addOperation(10, "CUT", "절단", 15, machine);
        entityManager.persist(routing);
        ProductionOrder order = ProductionOrder.create(
                routing,
                "PO-MAPPING-01",
                10,
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00"),
                OffsetDateTime.parse("2026-08-04T18:00:00+09:00"),
                80
        );
        entityManager.persist(order);
        entityManager.flush();
        Long orderId = order.id();
        entityManager.clear();

        ProductionOrder found =
                productionOrderRepository.findById(orderId).orElseThrow();
        entityManager.clear();
        ProductionOrderResponse response =
                ProductionOrderResponse.from(found);

        assertThat(response.productId()).isEqualTo(product.id());
        assertThat(response.routingId()).isEqualTo(routing.id());
        assertThat(response.status()).isEqualTo(ProductionOrderStatus.DRAFT);
    }
}
