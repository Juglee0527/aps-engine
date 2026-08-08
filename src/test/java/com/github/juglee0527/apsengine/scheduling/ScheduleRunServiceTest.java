package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.github.juglee0527.apsengine.capacity.WorkingCalendar;
import com.github.juglee0527.apsengine.capacity.WorkingCalendarRepository;
import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTime;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTimeRepository;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
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
import com.github.juglee0527.apsengine.product.routing.Operation;
import com.github.juglee0527.apsengine.product.routing.Routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScheduleRunServiceTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.of(
                    2026, 7, 27, 8, 0, 0, 0,
                    ZoneOffset.ofHours(9)
            );

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private WorkingCalendarRepository workingCalendarRepository;

    @Mock
    private ChangeoverTimeRepository changeoverTimeRepository;

    @Mock
    private MachineMaintenanceRepository maintenanceRepository;

    @Mock
    private ScheduleRunRepository scheduleRunRepository;

    private ScheduleRunService scheduleRunService;

    @BeforeEach
    void setUp() {
        scheduleRunService = new ScheduleRunService(
                productionOrderRepository,
                workingCalendarRepository,
                changeoverTimeRepository,
                maintenanceRepository,
                scheduleRunRepository
        );
    }

    @Test
    void executesAndBuildsPersistableScheduleRun() {
        TestData data = testData();
        UUID executionKey = UUID.randomUUID();
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(productionOrderRepository
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                        ProductionOrderStatus.CONFIRMED
        )).thenReturn(List.of(data.order(), data.order()));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(data.calendars());
        when(scheduleRunRepository.saveAndFlush(any(ScheduleRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRun scheduleRun = scheduleRunService.execute(
                executionKey,
                PLANNING_START
        );

        assertThat(scheduleRun.scheduledOperations()).hasSize(2);
        assertThat(scheduleRun.schedulingEnd())
                .isAfter(scheduleRun.planningStart());
        assertThat(data.order().status())
                .isEqualTo(ProductionOrderStatus.SCHEDULED);
    }

    @Test
    void returnsStoredRunForSameExecutionKey() {
        UUID executionKey = UUID.randomUUID();
        ScheduleRun stored = ScheduleRun.create(
                executionKey,
                new SchedulingPlan(
                        PLANNING_START,
                        PLANNING_START,
                        List.of()
                ),
                PLANNING_START
        );
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.of(stored));

        ScheduleRun result = scheduleRunService.execute(
                executionKey,
                PLANNING_START
        );

        assertThat(result).isSameAs(stored);
        verify(productionOrderRepository, never())
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(any());
    }

    @Test
    void rejectsEmptyPlanningScope() {
        assertThatThrownBy(() -> scheduleRunService.execute(
                UUID.randomUUID(),
                PLANNING_START,
                DispatchingRule.EDD,
                List.of()
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(
                        ErrorCode.INVALID_REQUEST
                )
        );
    }

    @Test
    void rejectsScopeContainingMissingOrNonConfirmedOrder() {
        UUID executionKey = UUID.randomUUID();
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(productionOrderRepository.findAllInScope(
                List.of(9L, 10L),
                ProductionOrderStatus.CONFIRMED
        )).thenReturn(List.of());

        assertThatThrownBy(() -> scheduleRunService.execute(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD,
                List.of(9L, 10L)
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.getMessage())
                        .contains("CONFIRMED")
        );
    }

    @Test
    void schedulesDuplicateScopeIdOnlyOnce() {
        TestData data = testData();
        UUID executionKey = UUID.randomUUID();
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(productionOrderRepository.findAllInScope(
                List.of(9L),
                ProductionOrderStatus.CONFIRMED
        )).thenReturn(List.of(data.order()));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(data.calendars());
        when(scheduleRunRepository.saveAndFlush(any(ScheduleRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRun result = scheduleRunService.execute(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD,
                List.of(9L, 9L)
        );

        assertThat(result.scheduledOperations()).hasSize(2);
        verify(productionOrderRepository).findAllInScope(
                List.of(9L),
                ProductionOrderStatus.CONFIRMED
        );
    }

    @Test
    void comparesRulesWithDifferentSequencesWithoutChangingOrders() {
        TestData data = testData();
        ProductionOrder urgentDue = ProductionOrder.create(
                data.order().routing(),
                "PO-DUE-FIRST",
                1,
                PLANNING_START,
                PLANNING_START.plusHours(4),
                10
        );
        ReflectionTestUtils.setField(urgentDue, "id", 10L);
        urgentDue.confirm();
        when(productionOrderRepository.findAllInScope(
                List.of(9L, 10L),
                ProductionOrderStatus.CONFIRMED
        )).thenReturn(List.of(data.order(), urgentDue));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(data.calendars());

        DispatchingRuleComparisonResponse comparison =
                scheduleRunService.compareDispatchingRules(
                        PLANNING_START,
                        List.of(9L, 10L)
                );

        DispatchingRuleComparisonResult priority = comparison.results()
                .stream()
                .filter(result -> result.dispatchingRule()
                        == DispatchingRule.EXPLICIT_PRIORITY)
                .findFirst()
                .orElseThrow();
        DispatchingRuleComparisonResult edd = comparison.results()
                .stream()
                .filter(result -> result.dispatchingRule()
                        == DispatchingRule.EDD)
                .findFirst()
                .orElseThrow();
        assertThat(priority.orderSequence().getFirst()).isEqualTo("PO-001");
        assertThat(edd.orderSequence().getFirst())
                .isEqualTo("PO-DUE-FIRST");
        assertThat(data.order().status())
                .isEqualTo(ProductionOrderStatus.CONFIRMED);
        assertThat(urgentDue.status())
                .isEqualTo(ProductionOrderStatus.CONFIRMED);
        verify(scheduleRunRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolvesComparisonTieInDocumentedRuleOrder() {
        TestData data = testData();
        when(productionOrderRepository.findAllInScope(
                List.of(9L),
                ProductionOrderStatus.CONFIRMED
        )).thenReturn(List.of(data.order()));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(data.calendars());

        DispatchingRuleComparisonResponse comparison =
                scheduleRunService.compareDispatchingRules(
                        PLANNING_START,
                        List.of(9L)
                );

        assertThat(comparison.recommendedRule())
                .isEqualTo(DispatchingRule.EXPLICIT_PRIORITY);
        assertThat(comparison.results())
                .extracting(
                        DispatchingRuleComparisonResult::dispatchingRule
                )
                .containsExactly(
                        DispatchingRule.EXPLICIT_PRIORITY,
                        DispatchingRule.EDD,
                        DispatchingRule.SPT
                );
    }

    @Test
    void reschedulesOnlyFutureTasksAndIncludesNewConfirmedOrder() {
        TestData data = testData();
        ScheduleRun source = sourceScheduleRun(data);
        ReflectionTestUtils.setField(source, "id", 10L);
        data.order().markScheduled();

        ProductionOrder newOrder = ProductionOrder.create(
                data.order().routing(),
                "PO-NEW",
                1,
                PLANNING_START,
                PLANNING_START.plusMinutes(30),
                90
        );
        ReflectionTestUtils.setField(newOrder, "id", 20L);
        newOrder.confirm();
        ProductionOrder foreignOrder = ProductionOrder.create(
                data.order().routing(),
                "PO-FOREIGN",
                1,
                PLANNING_START,
                PLANNING_START.plusHours(8),
                100
        );
        ReflectionTestUtils.setField(foreignOrder, "id", 21L);
        foreignOrder.confirm();
        UUID executionKey = UUID.randomUUID();
        OffsetDateTime frozenAt = PLANNING_START.plusHours(1);

        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(scheduleRunRepository.findById(10L))
                .thenReturn(Optional.of(source));
        when(productionOrderRepository
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                        ProductionOrderStatus.CONFIRMED
                )).thenReturn(List.of(foreignOrder, newOrder));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(data.calendars());
        when(scheduleRunRepository.saveAndFlush(any(ScheduleRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRun result = scheduleRunService.reschedule(
                10L,
                executionKey,
                frozenAt,
                null,
                List.of(data.order().id(), newOrder.id())
        );

        assertThat(source.scheduledOperations()).hasSize(2);
        assertThat(result.sourceScheduleRunId()).isEqualTo(10L);
        assertThat(result.frozenAt()).isEqualTo(frozenAt);
        assertThat(result.dispatchingRule())
                .isEqualTo(DispatchingRule.EXPLICIT_PRIORITY);
        assertThat(result.scheduledOperations()).hasSize(4);
        assertThat(result.scheduledOperations())
                .filteredOn(operation ->
                        operation.productionOrder().id().equals(
                                data.order().id()
                        )
                        && operation.operation().sequence() == 1)
                .singleElement()
                .satisfies(operation -> {
                    assertThat(operation.startAt())
                            .isEqualTo(PLANNING_START);
                    assertThat(operation.endAt())
                            .isEqualTo(frozenAt);
                });
        assertThat(result.scheduledOperations())
                .filteredOn(operation ->
                        operation.changeoverStartAt() == null
                                ? !operation.startAt().isBefore(frozenAt)
                                : !operation.changeoverStartAt()
                                        .isBefore(frozenAt))
                .hasSize(3);
        assertThat(newOrder.status())
                .isEqualTo(ProductionOrderStatus.SCHEDULED);
        assertThat(foreignOrder.status())
                .isEqualTo(ProductionOrderStatus.CONFIRMED);
        assertThat(result.scheduledOperations())
                .noneMatch(operation -> operation.productionOrder().id()
                        .equals(foreignOrder.id()));
        assertThat(result.totalTardinessMinutes()).isPositive();
    }

    @Test
    void keepsStartedTaskButDropsCancelledOrderFutureTask() {
        TestData data = testData();
        ScheduleRun source = sourceScheduleRun(data);
        ReflectionTestUtils.setField(source, "id", 11L);
        data.order().markScheduled();
        ReflectionTestUtils.setField(
                data.order(),
                "status",
                ProductionOrderStatus.CANCELLED
        );
        UUID executionKey = UUID.randomUUID();
        OffsetDateTime frozenAt = PLANNING_START.plusMinutes(30);

        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(scheduleRunRepository.findById(11L))
                .thenReturn(Optional.of(source));
        when(productionOrderRepository
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                        ProductionOrderStatus.CONFIRMED
                )).thenReturn(List.of());
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(data.calendars());
        when(scheduleRunRepository.saveAndFlush(any(ScheduleRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRun result = scheduleRunService.reschedule(
                11L,
                executionKey,
                frozenAt,
                DispatchingRule.EDD
        );

        assertThat(result.scheduledOperations())
                .singleElement()
                .satisfies(operation ->
                        assertThat(operation.operation().sequence())
                                .isEqualTo(1));
        assertThat(result.dispatchingRule()).isEqualTo(DispatchingRule.EDD);
        assertThat(data.order().status())
                .isEqualTo(ProductionOrderStatus.CANCELLED);
    }

    @Test
    void rejectsMachineWithoutWorkingCalendar() {
        TestData data = testData();
        UUID executionKey = UUID.randomUUID();
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(productionOrderRepository
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                        ProductionOrderStatus.CONFIRMED
                )).thenReturn(List.of(data.order()));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> scheduleRunService.execute(
                executionKey,
                PLANNING_START
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode())
                        .isEqualTo(ErrorCode.WORKING_CALENDAR_REQUIRED)
        );
    }

    @Test
    void loadsActiveChangeoverTimesIntoScheduler() {
        Factory factory = Factory.create("FACTORY-02", "전환 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-02", "전환 라인");
        Machine machine =
                Machine.create(line, "MACHINE-02", "전환 설비");
        ReflectionTestUtils.setField(machine, "id", 30L);
        Product productA =
                Product.create("PRODUCT-A", "제품 A", ProductUnit.PIECE);
        Product productB =
                Product.create("PRODUCT-B", "제품 B", ProductUnit.PIECE);
        ReflectionTestUtils.setField(productA, "id", 40L);
        ReflectionTestUtils.setField(productB, "id", 50L);
        ProductionOrder orderA = confirmedOrder(
                productA,
                machine,
                60L,
                70L,
                "PO-A",
                80
        );
        ProductionOrder orderB = confirmedOrder(
                productB,
                machine,
                61L,
                71L,
                "PO-B",
                70
        );
        ChangeoverTime changeoverTime =
                ChangeoverTime.create(machine, productA, productB, 30);
        MachineMaintenance maintenance = MachineMaintenance.create(
                machine,
                PLANNING_START.plusMinutes(15),
                PLANNING_START.plusMinutes(45),
                "정기 점검"
        );
        List<WorkingCalendar> calendars =
                weekdayCalendars(machine);
        UUID executionKey = UUID.randomUUID();
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(productionOrderRepository
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                        ProductionOrderStatus.CONFIRMED
                )).thenReturn(List.of(orderA, orderB));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(calendars);
        when(changeoverTimeRepository
                .findAllByMachine_IdInAndActiveTrue(anySet()))
                .thenReturn(List.of(changeoverTime));
        when(maintenanceRepository
                .findAllByMachine_IdInAndActiveTrueAndEndAtGreaterThanOrderByStartAtAsc(
                        anySet(),
                        any(OffsetDateTime.class)
                )).thenReturn(List.of(maintenance));
        when(scheduleRunRepository.saveAndFlush(any(ScheduleRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRun result = scheduleRunService.execute(
                executionKey,
                PLANNING_START
        );

        assertThat(result.scheduledOperations()).hasSize(2);
        assertThat(result.scheduledOperations().getFirst().endAt())
                .isEqualTo(PLANNING_START.plusMinutes(90));
        ScheduledOperation second =
                result.scheduledOperations().get(1);
        assertThat(second.productionOrder()).isSameAs(orderB);
        assertThat(second.changeoverMinutes()).isEqualTo(30);
        assertThat(second.changeoverStartAt()).isNotNull();
        assertThat(second.startAt())
                .isAfter(second.changeoverStartAt());
    }

    @Test
    void persistsAvailableAlternativeWhenPrimaryMachineIsStopped() {
        Factory factory = Factory.create("FACTORY-ALT", "대체 설비 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-ALT", "대체 설비 라인");
        Machine stoppedPrimary = Machine.create(
                line,
                "MACHINE-PRIMARY",
                "주 설비",
                MachineStatus.STOPPED
        );
        ReflectionTestUtils.setField(stoppedPrimary, "id", 101L);
        Machine alternative =
                Machine.create(line, "MACHINE-ALT", "대체 설비");
        ReflectionTestUtils.setField(alternative, "id", 102L);
        Product product =
                Product.create("PRODUCT-ALT", "대체 품목", ProductUnit.PIECE);
        ReflectionTestUtils.setField(product, "id", 103L);
        Routing routing =
                Routing.create(product, "ROUTING-ALT", "대체 Routing");
        routing.addOperation(
                1,
                "PROCESS",
                "가공",
                1,
                stoppedPrimary,
                Map.of(stoppedPrimary, 1, alternative, 2)
        );
        ReflectionTestUtils.setField(routing, "id", 104L);
        ReflectionTestUtils.setField(
                routing.operations().getFirst(),
                "id",
                105L
        );
        ProductionOrder order = ProductionOrder.create(
                routing,
                "PO-ALT",
                60,
                PLANNING_START,
                PLANNING_START.plusDays(2),
                80
        );
        ReflectionTestUtils.setField(order, "id", 106L);
        order.confirm();
        UUID executionKey = UUID.randomUUID();
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(productionOrderRepository
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                        ProductionOrderStatus.CONFIRMED
                )).thenReturn(List.of(order));
        when(workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(anyCollection()))
                .thenReturn(weekdayCalendars(alternative));
        when(scheduleRunRepository.saveAndFlush(any(ScheduleRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleRun result = scheduleRunService.execute(
                executionKey,
                PLANNING_START
        );

        assertThat(result.scheduledOperations())
                .singleElement()
                .satisfies(scheduled ->
                        assertThat(scheduled.machine())
                                .isSameAs(alternative));
    }

    @Test
    void rejectsOperationWithoutAvailableMachineCandidate() {
        Factory factory = Factory.create("FACTORY-STOP", "중지 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-STOP", "중지 라인");
        Machine stoppedMachine = Machine.create(
                line,
                "MACHINE-STOP",
                "중지 설비",
                MachineStatus.STOPPED
        );
        ReflectionTestUtils.setField(stoppedMachine, "id", 201L);
        Product product =
                Product.create("PRODUCT-STOP", "중지 품목", ProductUnit.PIECE);
        ReflectionTestUtils.setField(product, "id", 202L);
        ProductionOrder order = confirmedOrder(
                product,
                stoppedMachine,
                203L,
                204L,
                "PO-STOP",
                80
        );
        UUID executionKey = UUID.randomUUID();
        when(scheduleRunRepository.findByExecutionKey(executionKey))
                .thenReturn(Optional.empty());
        when(productionOrderRepository
                .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                        ProductionOrderStatus.CONFIRMED
                )).thenReturn(List.of(order));

        assertThatThrownBy(() -> scheduleRunService.execute(
                executionKey,
                PLANNING_START
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(
                        ErrorCode.MACHINE_UNAVAILABLE_FOR_SCHEDULING
                )
        );
    }

    private TestData testData() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ReflectionTestUtils.setField(factory, "id", 1L);
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        ReflectionTestUtils.setField(line, "id", 2L);
        Machine cutter =
                Machine.create(line, "CUTTER-01", "절단기");
        ReflectionTestUtils.setField(cutter, "id", 3L);
        Machine assembler =
                Machine.create(line, "ASSEMBLER-01", "조립기");
        ReflectionTestUtils.setField(assembler, "id", 4L);
        Product product =
                Product.create("PANEL", "제어 패널", ProductUnit.PIECE);
        ReflectionTestUtils.setField(product, "id", 5L);
        Routing routing =
                Routing.create(product, "PANEL-STD", "표준 공정");
        routing.addOperation(1, "CUT", "절단", 10, cutter);
        routing.addOperation(2, "ASSEMBLE", "조립", 20, assembler);
        ReflectionTestUtils.setField(routing, "id", 6L);
        List<Operation> operations = routing.operations();
        ReflectionTestUtils.setField(operations.get(0), "id", 7L);
        ReflectionTestUtils.setField(operations.get(1), "id", 8L);
        ProductionOrder order = ProductionOrder.create(
                routing,
                "PO-001",
                2,
                PLANNING_START,
                PLANNING_START.plusDays(2),
                80
        );
        ReflectionTestUtils.setField(order, "id", 9L);
        order.confirm();

        List<WorkingCalendar> calendars = new ArrayList<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            if (dayOfWeek == DayOfWeek.SATURDAY
                    || dayOfWeek == DayOfWeek.SUNDAY) {
                continue;
            }
            calendars.add(WorkingCalendar.create(
                    cutter,
                    dayOfWeek,
                    LocalTime.of(8, 0),
                    LocalTime.of(17, 0)
            ));
            calendars.add(WorkingCalendar.create(
                    assembler,
                    dayOfWeek,
                    LocalTime.of(8, 0),
                    LocalTime.of(17, 0)
            ));
        }
        return new TestData(order, List.copyOf(calendars));
    }

    private ScheduleRun sourceScheduleRun(TestData data) {
        List<Operation> operations = data.order().routing().operations();
        ScheduledTask firstTask = new ScheduledTask(
                data.order().id(),
                data.order().orderNumber(),
                operations.get(0).id(),
                operations.get(0).machine().id(),
                operations.get(0).sequence(),
                operations.get(0).code(),
                operations.get(0).name(),
                null,
                0,
                PLANNING_START,
                PLANNING_START.plusHours(1),
                60,
                data.order().dueAt(),
                false
        );
        ScheduledTask secondTask = new ScheduledTask(
                data.order().id(),
                data.order().orderNumber(),
                operations.get(1).id(),
                operations.get(1).machine().id(),
                operations.get(1).sequence(),
                operations.get(1).code(),
                operations.get(1).name(),
                null,
                0,
                PLANNING_START.plusHours(1),
                PLANNING_START.plusHours(2),
                60,
                data.order().dueAt(),
                false
        );
        ScheduleRun source = ScheduleRun.create(
                UUID.randomUUID(),
                new SchedulingPlan(
                        PLANNING_START,
                        secondTask.endAt(),
                        List.of(firstTask, secondTask)
                ),
                PLANNING_START
        );
        source.addScheduledOperation(
                data.order(),
                operations.get(0),
                operations.get(0).machine(),
                firstTask
        );
        source.addScheduledOperation(
                data.order(),
                operations.get(1),
                operations.get(1).machine(),
                secondTask
        );
        return source;
    }

    private ProductionOrder confirmedOrder(
            Product product,
            Machine machine,
            long routingId,
            long operationId,
            String orderNumber,
            int priority
    ) {
        Routing routing =
                Routing.create(product, orderNumber + "-ROUTE", "표준 공정");
        routing.addOperation(1, "PROCESS", "가공", 1, machine);
        ReflectionTestUtils.setField(routing, "id", routingId);
        ReflectionTestUtils.setField(
                routing.operations().getFirst(),
                "id",
                operationId
        );
        ProductionOrder order = ProductionOrder.create(
                routing,
                orderNumber,
                60,
                PLANNING_START,
                PLANNING_START.plusDays(2),
                priority
        );
        ReflectionTestUtils.setField(order, "id", routingId + 100);
        order.confirm();
        return order;
    }

    private List<WorkingCalendar> weekdayCalendars(Machine machine) {
        List<WorkingCalendar> calendars = new ArrayList<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            if (dayOfWeek != DayOfWeek.SATURDAY
                    && dayOfWeek != DayOfWeek.SUNDAY) {
                calendars.add(WorkingCalendar.create(
                        machine,
                        dayOfWeek,
                        LocalTime.of(8, 0),
                        LocalTime.of(17, 0)
                ));
            }
        }
        return List.copyOf(calendars);
    }

    private record TestData(
            ProductionOrder order,
            List<WorkingCalendar> calendars
    ) {
    }
}
