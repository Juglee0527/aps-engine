package com.github.juglee0527.apsengine.constraint.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

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
class MachineMaintenanceJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MachineMaintenanceRepository maintenanceRepository;

    @Test
    void persistsAdjacentMaintenanceWindows() {
        Machine machine = persistMachine();
        OffsetDateTime start =
                OffsetDateTime.parse("2026-08-03T10:00:00+09:00");
        entityManager.persist(MachineMaintenance.create(
                machine,
                start,
                start.plusHours(1),
                "1차 점검"
        ));
        entityManager.persist(MachineMaintenance.create(
                machine,
                start.plusHours(1),
                start.plusHours(2),
                "2차 점검"
        ));
        entityManager.flush();
        entityManager.clear();

        assertThat(maintenanceRepository
                .findAllByMachine_IdAndActiveTrueOrderByStartAtAsc(
                        machine.id()
                )).hasSize(2);
    }

    @Test
    void rejectsOverlappingMaintenanceAtDatabaseBoundary() {
        Machine machine = persistMachine();
        OffsetDateTime start =
                OffsetDateTime.parse("2026-08-03T10:00:00+09:00");
        entityManager.persist(MachineMaintenance.create(
                machine,
                start,
                start.plusHours(2),
                "기존 점검"
        ));
        entityManager.flush();
        assertThatThrownBy(() -> {
            entityManager.persist(MachineMaintenance.create(
                    machine,
                    start.plusHours(1),
                    start.plusHours(3),
                    "겹치는 점검"
            ));
            entityManager.flush();
        })
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining(
                        "ex_machine_maintenance_no_overlap"
                );
    }

    private Machine persistMachine() {
        Factory factory =
                Factory.create("FACTORY-MAINTENANCE", "정비 공장");
        entityManager.persist(factory);
        ProductionLine line = ProductionLine.create(
                factory,
                "LINE-MAINTENANCE",
                "정비 라인"
        );
        entityManager.persist(line);
        Machine machine = Machine.create(
                line,
                "MACHINE-MAINTENANCE",
                "정비 설비"
        );
        entityManager.persist(machine);
        return machine;
    }
}
