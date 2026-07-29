package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.github.juglee0527.apsengine.capacity.WorkingCalendar;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderStatus;
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
class ScheduleRunJpaIntegrationTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.of(
                    2026, 7, 27, 8, 0, 0, 0,
                    ZoneOffset.ofHours(9)
            );

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ScheduleRunService scheduleRunService;

    @Autowired
    private ScheduleRunRepository scheduleRunRepository;

    @Autowired
    private ProductionOrderRepository productionOrderRepository;

    @Test
    void persistsScheduleAndMarksOrderAsScheduled() {
        ProductionOrder order = persistConfirmedOrder();
        UUID executionKey = UUID.randomUUID();

        ScheduleRun created = scheduleRunService.execute(
                executionKey,
                PLANNING_START
        );
        Long scheduleRunId = created.id();
        Long orderId = order.id();
        entityManager.flush();
        entityManager.clear();

        ScheduleRun stored = scheduleRunRepository
                .findById(scheduleRunId)
                .orElseThrow();
        ProductionOrder storedOrder = productionOrderRepository
                .findById(orderId)
                .orElseThrow();

        assertThat(stored.executionKey()).isEqualTo(executionKey);
        assertThat(stored.scheduledOperations()).hasSize(2);
        assertThat(storedOrder.status())
                .isEqualTo(ProductionOrderStatus.SCHEDULED);
    }

    private ProductionOrder persistConfirmedOrder() {
        Factory factory =
                Factory.create("FACTORY-SCHEDULE", "스케줄 공장");
        entityManager.persist(factory);
        ProductionLine line =
                ProductionLine.create(
                        factory,
                        "LINE-SCHEDULE",
                        "스케줄 라인"
                );
        entityManager.persist(line);
        Machine cutter =
                Machine.create(line, "CUTTER-SCHEDULE", "절단기");
        entityManager.persist(cutter);
        Machine assembler =
                Machine.create(line, "ASSEMBLER-SCHEDULE", "조립기");
        entityManager.persist(assembler);
        Product product = Product.create(
                "PRODUCT-SCHEDULE",
                "스케줄 품목",
                ProductUnit.PIECE
        );
        entityManager.persist(product);
        Routing routing =
                Routing.create(product, "ROUTING-SCHEDULE", "표준 공정");
        routing.addOperation(1, "CUT", "절단", 10, cutter);
        routing.addOperation(2, "ASSEMBLE", "조립", 20, assembler);
        entityManager.persist(routing);
        persistWeekdayCalendars(cutter);
        persistWeekdayCalendars(assembler);
        ProductionOrder order = ProductionOrder.create(
                routing,
                "PO-SCHEDULE",
                2,
                PLANNING_START,
                PLANNING_START.plusDays(2),
                80
        );
        order.confirm();
        entityManager.persist(order);
        entityManager.flush();
        return order;
    }

    private void persistWeekdayCalendars(Machine machine) {
        List<DayOfWeek> weekdays = List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
        for (DayOfWeek dayOfWeek : weekdays) {
            entityManager.persist(WorkingCalendar.create(
                    machine,
                    dayOfWeek,
                    LocalTime.of(8, 0),
                    LocalTime.of(17, 0)
            ));
        }
    }
}
