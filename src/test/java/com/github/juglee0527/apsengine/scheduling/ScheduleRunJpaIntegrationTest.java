package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.juglee0527.apsengine.capacity.BottleneckAnalysis;
import com.github.juglee0527.apsengine.capacity.BottleneckService;
import com.github.juglee0527.apsengine.capacity.WorkingCalendar;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTime;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenance;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineStatus;
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

    @Autowired
    private PlannedLeadTimeService plannedLeadTimeService;

    @Autowired
    private BottleneckService bottleneckService;

    @Test
    void persistsScheduleAndMarksOrderAsScheduled() {
        ProductionOrder order = persistConfirmedOrder();
        ProductionOrder secondOrder = ProductionOrder.create(
                order.routing(),
                "PO-SCHEDULE-SECOND",
                1,
                PLANNING_START,
                PLANNING_START.plusDays(3),
                50
        );
        secondOrder.confirm();
        entityManager.persist(secondOrder);
        entityManager.flush();
        UUID executionKey = UUID.randomUUID();

        ScheduleRun created = scheduleRunService.execute(
                executionKey,
                PLANNING_START
        );
        Long scheduleRunId = created.id();
        Long orderId = order.id();
        Long secondOrderId = secondOrder.id();
        entityManager.flush();
        entityManager.clear();

        ScheduleRun stored = scheduleRunRepository
                .findById(scheduleRunId)
                .orElseThrow();
        ProductionOrder storedOrder = productionOrderRepository
                .findById(orderId)
                .orElseThrow();
        ProductionOrder storedSecondOrder = productionOrderRepository
                .findById(secondOrderId)
                .orElseThrow();

        assertThat(stored.executionKey()).isEqualTo(executionKey);
        assertThat(stored.planningOffsetSeconds()).isEqualTo(32_400);
        assertThat(stored.dispatchingRule())
                .isEqualTo(DispatchingRule.EXPLICIT_PRIORITY);
        assertThat(stored.makespanMinutes()).isPositive();
        assertThat(stored.machineUtilizationPercent()).isPositive();
        assertThat(stored.scheduledOperations())
                .filteredOn(scheduledOperation ->
                        scheduledOperation.productionOrder().id()
                                .equals(orderId)
                                || scheduledOperation.productionOrder().id()
                                .equals(secondOrderId))
                .hasSize(4);
        assertThat(storedOrder.status())
                .isEqualTo(ProductionOrderStatus.SCHEDULED);
        assertThat(storedSecondOrder.status())
                .isEqualTo(ProductionOrderStatus.SCHEDULED);
    }

    @Test
    void persistsRescheduleTraceAndKeepsFrozenOperation() {
        ProductionOrder sourceOrder = persistConfirmedOrder();
        ScheduleRun source = scheduleRunService.execute(
                UUID.randomUUID(),
                PLANNING_START,
                DispatchingRule.EXPLICIT_PRIORITY
        );
        ScheduledOperation frozenSourceOperation =
                source.scheduledOperations().getFirst();
        OffsetDateTime frozenAt = frozenSourceOperation.endAt();

        ProductionOrder newOrder = ProductionOrder.create(
                sourceOrder.routing(),
                "PO-RESCHEDULE-NEW",
                1,
                PLANNING_START,
                PLANNING_START.plusDays(3),
                90
        );
        newOrder.confirm();
        entityManager.persist(newOrder);
        entityManager.flush();

        ScheduleRun rescheduled = scheduleRunService.reschedule(
                source.id(),
                UUID.randomUUID(),
                frozenAt,
                DispatchingRule.EDD
        );
        Long rescheduledId = rescheduled.id();
        entityManager.flush();
        entityManager.clear();

        ScheduleRun stored = scheduleRunRepository
                .findById(rescheduledId)
                .orElseThrow();
        ScheduleRun storedSource = scheduleRunRepository
                .findById(source.id())
                .orElseThrow();
        ProductionOrder storedNewOrder = productionOrderRepository
                .findById(newOrder.id())
                .orElseThrow();

        assertThat(stored.sourceScheduleRunId()).isEqualTo(source.id());
        assertThat(stored.frozenAt().toInstant())
                .isEqualTo(frozenAt.toInstant());
        assertThat(stored.dispatchingRule()).isEqualTo(DispatchingRule.EDD);
        assertThat(storedSource.scheduledOperations()).hasSize(2);
        assertThat(stored.scheduledOperations()).hasSize(4);
        assertThat(stored.scheduledOperations())
                .filteredOn(operation ->
                        operation.productionOrder().id()
                                .equals(sourceOrder.id())
                        && operation.operation().id().equals(
                                frozenSourceOperation.operation().id()
                        ))
                .singleElement()
                .satisfies(operation -> {
                    assertThat(operation.startAt().toInstant())
                            .isEqualTo(
                                    frozenSourceOperation.startAt().toInstant()
                            );
                    assertThat(operation.endAt().toInstant())
                            .isEqualTo(
                                    frozenSourceOperation.endAt().toInstant()
                            );
                });
        assertThat(storedNewOrder.status())
                .isEqualTo(ProductionOrderStatus.SCHEDULED);
    }

    @Test
    void persistsAppliedChangeoverTime() {
        Factory factory =
                Factory.create("FACTORY-CHANGEOVER-RUN", "전환 실행 공장");
        entityManager.persist(factory);
        ProductionLine line = ProductionLine.create(
                factory,
                "LINE-CHANGEOVER-RUN",
                "전환 실행 라인"
        );
        entityManager.persist(line);
        Machine machine = Machine.create(
                line,
                "MACHINE-CHANGEOVER-RUN",
                "전환 실행 설비"
        );
        entityManager.persist(machine);
        Product productA = Product.create(
                "PRODUCT-CHANGEOVER-A",
                "전환 품목 A",
                ProductUnit.PIECE
        );
        Product productB = Product.create(
                "PRODUCT-CHANGEOVER-B",
                "전환 품목 B",
                ProductUnit.PIECE
        );
        entityManager.persist(productA);
        entityManager.persist(productB);
        Routing routingA =
                Routing.create(productA, "ROUTE-CHANGEOVER-A", "A 공정");
        routingA.addOperation(1, "PROCESS-A", "A 가공", 30, machine);
        Routing routingB =
                Routing.create(productB, "ROUTE-CHANGEOVER-B", "B 공정");
        routingB.addOperation(1, "PROCESS-B", "B 가공", 30, machine);
        entityManager.persist(routingA);
        entityManager.persist(routingB);
        persistWeekdayCalendars(machine);
        entityManager.persist(ChangeoverTime.create(
                machine,
                productA,
                productB,
                45
        ));
        entityManager.persist(MachineMaintenance.create(
                machine,
                PLANNING_START.plusMinutes(15),
                PLANNING_START.plusMinutes(45),
                "정기 점검"
        ));
        ProductionOrder orderA = ProductionOrder.create(
                routingA,
                "PO-CHANGEOVER-A",
                1,
                PLANNING_START,
                PLANNING_START.plusDays(1),
                100
        );
        ProductionOrder orderB = ProductionOrder.create(
                routingB,
                "PO-CHANGEOVER-B",
                1,
                PLANNING_START,
                PLANNING_START.plusDays(2),
                100
        );
        orderA.confirm();
        orderB.confirm();
        entityManager.persist(orderA);
        entityManager.persist(orderB);
        entityManager.flush();

        ScheduleRun created = scheduleRunService.execute(
                UUID.randomUUID(),
                PLANNING_START,
                DispatchingRule.EDD
        );
        Long scheduleRunId = created.id();
        Long orderBId = orderB.id();
        entityManager.flush();
        entityManager.clear();

        ScheduleRun storedRun = scheduleRunRepository
                .findById(scheduleRunId)
                .orElseThrow();
        ScheduledOperation storedOperation = storedRun
                .scheduledOperations()
                .stream()
                .filter(operation -> operation.productionOrder().id()
                        .equals(orderBId))
                .findFirst()
                .orElseThrow();

        assertThat(storedOperation.changeoverMinutes()).isEqualTo(45);
        assertThat(storedOperation.changeoverStartAt()).isNotNull();
        assertThat(storedOperation.startAt())
                .isEqualTo(PLANNING_START.plusMinutes(105));
        assertThat(storedOperation.startAt())
                .isAfter(storedOperation.changeoverStartAt());

        PlannedLeadTime leadTime = plannedLeadTimeService
                .calculate(scheduleRunId)
                .stream()
                .filter(result -> result.productionOrderId()
                        == orderBId)
                .findFirst()
                .orElseThrow();
        assertThat(leadTime.plannedLeadTimeMinutes()).isEqualTo(135);
        assertThat(leadTime.processingMinutes()).isEqualTo(30);
        assertThat(leadTime.changeoverMinutes()).isEqualTo(45);
        assertThat(leadTime.waitingMinutes()).isEqualTo(60);

        BottleneckAnalysis bottlenecks =
                bottleneckService.detect(scheduleRunId);
        assertThat(bottlenecks.candidates()).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.machineId())
                            .isEqualTo(machine.id());
                    assertThat(candidate.availableMinutes())
                            .isEqualTo(105);
                    assertThat(candidate.loadMinutes()).isEqualTo(105);
                    assertThat(candidate.utilizationPercent())
                            .isEqualByComparingTo("100.00");
                });
    }

    @Test
    void persistsSelectedAlternativeMachine() {
        Factory factory =
                Factory.create("FACTORY-ALT-RUN", "대체 실행 공장");
        entityManager.persist(factory);
        ProductionLine line = ProductionLine.create(
                factory,
                "LINE-ALT-RUN",
                "대체 실행 라인"
        );
        entityManager.persist(line);
        Machine primaryMachine = Machine.create(
                line,
                "MACHINE-ALT-PRIMARY",
                "중지 주 설비",
                MachineStatus.STOPPED
        );
        Machine alternativeMachine = Machine.create(
                line,
                "MACHINE-ALT-AVAILABLE",
                "가용 대체 설비"
        );
        entityManager.persist(primaryMachine);
        entityManager.persist(alternativeMachine);
        Product product = Product.create(
                "PRODUCT-ALT-RUN",
                "대체 실행 품목",
                ProductUnit.PIECE
        );
        entityManager.persist(product);
        Routing routing =
                Routing.create(product, "ROUTING-ALT-RUN", "대체 공정");
        routing.addOperation(
                1,
                "PROCESS-ALT",
                "대체 가공",
                30,
                primaryMachine,
                Map.of(primaryMachine, 1, alternativeMachine, 2)
        );
        entityManager.persist(routing);
        persistWeekdayCalendars(alternativeMachine);
        ProductionOrder order = ProductionOrder.create(
                routing,
                "PO-ALT-RUN",
                1,
                PLANNING_START,
                PLANNING_START.plusDays(1),
                100
        );
        order.confirm();
        entityManager.persist(order);
        entityManager.flush();
        Long orderId = order.id();

        ScheduleRun created = scheduleRunService.execute(
                UUID.randomUUID(),
                PLANNING_START,
                DispatchingRule.EDD
        );
        Long scheduleRunId = created.id();
        Long alternativeMachineId = alternativeMachine.id();
        entityManager.flush();
        entityManager.clear();

        ScheduleRun storedRun = scheduleRunRepository
                .findById(scheduleRunId)
                .orElseThrow();
        ScheduledOperation storedOperation = storedRun
                .scheduledOperations()
                .stream()
                .filter(operation -> operation.productionOrder().id()
                        .equals(orderId))
                .findFirst()
                .orElseThrow();

        assertThat(storedOperation.machine().id())
                .isEqualTo(alternativeMachineId);
        assertThat(storedRun.dispatchingRule())
                .isEqualTo(DispatchingRule.EDD);
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
