package com.github.juglee0527.apsengine.product.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;

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
class RoutingJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RoutingRepository routingRepository;

    @Test
    void persistsAndLoadsDetailedRouting() {
        Product product =
                Product.create("PRODUCT-RT-MAP", "매핑 품목", ProductUnit.PIECE);
        entityManager.persist(product);
        Factory factory = Factory.create("FACTORY-RT-MAP", "매핑 공장");
        entityManager.persist(factory);
        ProductionLine line =
                ProductionLine.create(factory, "LINE-RT", "매핑 라인");
        entityManager.persist(line);
        Machine machine = Machine.create(line, "MACHINE-RT", "매핑 설비");
        entityManager.persist(machine);
        Machine alternativeMachine =
                Machine.create(line, "MACHINE-RT-ALT", "대체 설비");
        entityManager.persist(alternativeMachine);
        Routing routing =
                Routing.create(product, "ROUTING-01", "표준 Routing");
        routing.addOperation(
                10,
                "CUT",
                "절단",
                15,
                machine,
                Map.of(machine, 1, alternativeMachine, 1)
        );
        entityManager.persist(routing);
        entityManager.flush();
        Long routingId = routing.id();
        entityManager.clear();

        Routing found = routingRepository.findDetailById(routingId)
                .orElseThrow();
        entityManager.clear();
        RoutingResponse response = RoutingResponse.from(found);

        assertThat(response.productId()).isEqualTo(product.id());
        assertThat(response.operations()).hasSize(1);
        assertThat(response.operations().getFirst().machineId())
                .isEqualTo(machine.id());
        assertThat(response.operations().getFirst().machineCandidates())
                .hasSize(2)
                .extracting(OperationMachineCandidateResponse::machineId)
                .containsExactlyInAnyOrder(
                        machine.id(),
                        alternativeMachine.id()
                );
    }
}
