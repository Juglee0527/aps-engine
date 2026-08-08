package com.github.juglee0527.apsengine.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;
import com.github.juglee0527.apsengine.support.PostgreSqlContainerIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Rollback
class ProductionOrderSearchPostgreSqlIntegrationTest
        extends PostgreSqlContainerIntegrationTest {

    @Autowired FactoryRepository factoryRepository;
    @Autowired ProductionLineRepository lineRepository;
    @Autowired MachineRepository machineRepository;
    @Autowired ProductRepository productRepository;
    @Autowired RoutingRepository routingRepository;
    @Autowired ProductionOrderRepository orderRepository;

    @Test
    void searchesWithoutKeywordUsingPostgreSqlTextParameters() {
        ProductionOrder order = createOrder();

        Page<ProductionOrder> result = orderRepository.search(
                "",
                null,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(ProductionOrder::orderNumber)
                .contains(order.orderNumber());
    }

    private ProductionOrder createOrder() {
        Factory factory = factoryRepository.saveAndFlush(
                Factory.create("SEARCH-F", "Search Test Factory")
        );
        ProductionLine line = lineRepository.saveAndFlush(
                ProductionLine.create(factory, "SEARCH-L", "Search Test Line")
        );
        Machine machine = machineRepository.saveAndFlush(
                Machine.create(line, "SEARCH-M", "Search Test Machine")
        );
        Product product = productRepository.saveAndFlush(
                Product.create("SEARCH-P", "Search Test Product", ProductUnit.PIECE)
        );
        Routing routing = Routing.create(product, "STD", "Search Test Routing");
        routing.addOperation(1, "OP", "Processing", 10, machine);
        routing = routingRepository.saveAndFlush(routing);
        ProductionOrder order = ProductionOrder.create(
                routing,
                "SEARCH-PO",
                1,
                OffsetDateTime.parse("2026-08-10T08:00:00+09:00"),
                OffsetDateTime.parse("2026-08-10T12:00:00+09:00"),
                50
        );
        order.confirm();
        return orderRepository.saveAndFlush(order);
    }
}
