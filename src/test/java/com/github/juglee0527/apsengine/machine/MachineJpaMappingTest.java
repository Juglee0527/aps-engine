package com.github.juglee0527.apsengine.machine;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
class MachineJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MachineRepository machineRepository;

    @Test
    void persistsAndLoadsMachine() {
        Factory factory = Factory.create("FACTORY-M-10", "매핑 테스트 공장");
        entityManager.persist(factory);
        ProductionLine productionLine =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        entityManager.persist(productionLine);
        Machine machine =
                Machine.create(productionLine, "MACHINE-01", "절단 설비");
        entityManager.persist(machine);
        entityManager.flush();

        Long machineId = machine.id();
        entityManager.clear();

        Machine foundMachine = entityManager.find(Machine.class, machineId);

        assertThat(foundMachine).isNotNull();
        assertThat(foundMachine.productionLine().id())
                .isEqualTo(productionLine.id());
        assertThat(foundMachine.code()).isEqualTo("MACHINE-01");
        assertThat(foundMachine.name()).isEqualTo("절단 설비");
        assertThat(foundMachine.status()).isEqualTo(MachineStatus.AVAILABLE);
    }

    @Test
    void loadsProductionLineForPageResponseAfterPersistenceContextCloses() {
        Factory factory =
                Factory.create("FACTORY-M-PAGE", "목록 조회 테스트 공장");
        entityManager.persist(factory);
        ProductionLine productionLine = ProductionLine.create(
                factory,
                "LINE-PAGE",
                "목록 조회 테스트 라인"
        );
        entityManager.persist(productionLine);
        Machine machine =
                Machine.create(productionLine, "MACHINE-PAGE", "테스트 설비");
        entityManager.persist(machine);
        entityManager.flush();
        entityManager.clear();

        Page<Machine> page = machineRepository.findAllByProductionLine_Id(
                productionLine.id(),
                PageRequest.of(0, 20)
        );
        entityManager.clear();

        MachineResponse response =
                MachineResponse.from(page.getContent().getFirst());

        assertThat(response.productionLineId())
                .isEqualTo(productionLine.id());
    }
}
