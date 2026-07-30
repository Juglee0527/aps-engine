package com.github.juglee0527.apsengine.constraint.changeover;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
class ChangeoverTimeJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ChangeoverTimeRepository changeoverTimeRepository;

    @Test
    void persistsAndLoadsDirectionalMapping() {
        Factory factory = Factory.create("FACTORY-CO-MAP", "전환 공장");
        entityManager.persist(factory);
        ProductionLine line =
                ProductionLine.create(factory, "LINE-CO", "전환 라인");
        entityManager.persist(line);
        Machine machine =
                Machine.create(line, "MACHINE-CO", "전환 설비");
        entityManager.persist(machine);
        Product fromProduct =
                Product.create("PRODUCT-CO-A", "제품 A", ProductUnit.PIECE);
        Product toProduct =
                Product.create("PRODUCT-CO-B", "제품 B", ProductUnit.PIECE);
        entityManager.persist(fromProduct);
        entityManager.persist(toProduct);
        entityManager.persist(ChangeoverTime.create(
                machine,
                fromProduct,
                toProduct,
                30
        ));
        entityManager.flush();
        entityManager.clear();

        List<ChangeoverTime> mappings = changeoverTimeRepository
                .findAllByMachine_IdAndActiveTrueOrderByFromProduct_IdAscToProduct_IdAsc(
                        machine.id()
                );
        ChangeoverTimeResponse response =
                ChangeoverTimeResponse.from(mappings.getFirst());

        assertThat(response.machineId()).isEqualTo(machine.id());
        assertThat(response.fromProductCode()).isEqualTo("PRODUCT-CO-A");
        assertThat(response.toProductCode()).isEqualTo("PRODUCT-CO-B");
        assertThat(response.changeoverMinutes()).isEqualTo(30);
    }
}
