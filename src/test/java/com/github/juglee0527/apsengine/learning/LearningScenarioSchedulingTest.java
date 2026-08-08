package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;
import com.github.juglee0527.apsengine.capacity.UnavailableInterval;
import com.github.juglee0527.apsengine.scheduling.ForwardScheduler;
import com.github.juglee0527.apsengine.scheduling.EarliestDueDateRule;
import com.github.juglee0527.apsengine.scheduling.ScheduleKpiCalculator;
import com.github.juglee0527.apsengine.scheduling.ScheduleKpis;
import com.github.juglee0527.apsengine.scheduling.SchedulingOperationInput;
import com.github.juglee0527.apsengine.scheduling.SchedulingMachineCandidateInput;
import com.github.juglee0527.apsengine.scheduling.SchedulingChangeoverInput;
import com.github.juglee0527.apsengine.scheduling.SchedulingOrderInput;
import com.github.juglee0527.apsengine.scheduling.SchedulingPlan;
import com.github.juglee0527.apsengine.scheduling.ShortestProcessingTimeRule;

import org.junit.jupiter.api.Test;

class LearningScenarioSchedulingTest {

    private static final OffsetDateTime START = OffsetDateTime.parse(
            "2026-08-10T08:00:00+09:00"
    );
    private final LearningScenarioCatalog catalog =
            new LearningScenarioCatalog();
    private final ForwardScheduler scheduler = new ForwardScheduler();

    @Test
    void firstPlanSelectsDefinedMachinesAndPriorityOrder() {
        ScenarioInput input = input("FIRST_PLAN");

        SchedulingPlan plan = scheduler.schedule(START, input.orders());

        assertThat(plan.tasks().getFirst().orderNumber()).isEqualTo("PO-01");
        assertThat(plan.tasks().stream()
                .filter(task -> task.orderNumber().equals("PO-01"))
                .map(task -> task.machineId())
                .toList()).containsExactly(
                        input.machineIds().get("CUT"),
                        input.machineIds().get("ASM")
                );
    }

    @Test
    void finiteCapacitySerializesWorkAndCrossesWorkingDay() {
        ScenarioInput input = input("FINITE_CAPACITY");

        SchedulingPlan plan = scheduler.schedule(START, input.orders());

        assertThat(plan.tasks()).hasSize(4);
        for (int index = 1; index < plan.tasks().size(); index++) {
            assertThat(plan.tasks().get(index).startAt())
                    .isAfterOrEqualTo(plan.tasks().get(index - 1).endAt());
        }
        assertThat(plan.schedulingEnd().toLocalDate())
                .isAfter(START.toLocalDate());
    }

    @Test
    void precedenceKeepsThreeOperationsInOrder() {
        ScenarioInput input = input("PRECEDENCE");

        SchedulingPlan plan = scheduler.schedule(START, input.orders());

        assertThat(plan.tasks().stream()
                .filter(task -> task.orderNumber().equals("PR-01"))
                .map(task -> task.sequence())
                .toList()).containsExactly(1, 2, 3);
        assertThat(plan.tasks().stream()
                .filter(task -> task.orderNumber().equals("PR-01"))
                .map(task -> task.machineId())
                .toList()).containsExactly(
                        input.machineIds().get("MAKE"),
                        input.machineIds().get("TEST"),
                        input.machineIds().get("PACK")
                );
    }

    @Test
    void tardinessProducesDelayedOrdersAndKpi() {
        ScenarioInput input = input("TARDINESS");
        SchedulingPlan plan = scheduler.schedule(START, input.orders());

        ScheduleKpis kpis = new ScheduleKpiCalculator().calculate(
                plan,
                input.orders()
        );

        assertThat(kpis.delayedOrderCount()).isGreaterThan(0);
        assertThat(kpis.totalTardinessMinutes()).isGreaterThan(0);
    }

    @Test
    void comparisonScenarioProducesThreeDifferentFirstOrders() {
        ScenarioInput input = input("RULE_COMPARISON");

        String priorityFirst = scheduler.schedule(START, input.orders())
                .tasks().getFirst().orderNumber();
        String eddFirst = new ForwardScheduler(new EarliestDueDateRule())
                .schedule(START, input.orders())
                .tasks().getFirst().orderNumber();
        String sptFirst = new ForwardScheduler(
                new ShortestProcessingTimeRule()
        ).schedule(START, input.orders())
                .tasks().getFirst().orderNumber();

        assertThat(priorityFirst).isEqualTo("RC-LONG-1");
        assertThat(eddFirst).isEqualTo("RC-MED-1");
        assertThat(sptFirst).isEqualTo("RC-SHORT-1");
    }

    @Test
    void directionalChangeoverAddsExpectedPreparationMinutes() {
        ScenarioInput input = input("CHANGEOVER");
        SchedulingPlan plan = scheduler.schedule(
                START,
                input.orders(),
                List.of(
                        new SchedulingChangeoverInput(
                                input.machineIds().get("CELL"),
                                input.productIds().get("ITEM-A"),
                                input.productIds().get("ITEM-B"),
                                120
                        ),
                        new SchedulingChangeoverInput(
                                input.machineIds().get("CELL"),
                                input.productIds().get("ITEM-B"),
                                input.productIds().get("ITEM-A"),
                                15
                        )
                )
        );

        assertThat(plan.tasks())
                .extracting(task -> task.changeoverMinutes())
                .containsExactly(0L, 120L, 15L, 120L);
    }

    @Test
    void maintenanceMovesCompletionPastUnavailableWindow() {
        ScenarioInput input = input("MAINTENANCE");

        SchedulingPlan plan = scheduler.schedule(START, input.orders());

        assertThat(plan.tasks().get(1).endAt())
                .isEqualTo(START.plusHours(6));
        assertThat(plan.tasks()).allSatisfy(task ->
                assertThat(task.workingMinutes()).isEqualTo(90));
    }

    @Test
    void alternativeMachineDistributesQueuedOperations() {
        ScenarioInput input = input("ALTERNATIVE_MACHINE");

        SchedulingPlan plan = scheduler.schedule(START, input.orders());

        assertThat(plan.tasks().stream().map(task -> task.machineId()))
                .contains(
                        input.machineIds().get("PRIMARY"),
                        input.machineIds().get("ALT")
                );
    }

    @Test
    void bottleneckMachineCarriesHighestLoad() {
        ScenarioInput input = input("BOTTLENECK");

        SchedulingPlan plan = scheduler.schedule(START, input.orders());

        Map<Long, Long> loadByMachine = new LinkedHashMap<>();
        plan.tasks().forEach(task -> loadByMachine.merge(
                task.machineId(),
                task.workingMinutes(),
                Long::sum
        ));
        assertThat(loadByMachine.get(input.machineIds().get("HEAT")))
                .isGreaterThan(loadByMachine.get(
                        input.machineIds().get("PREP")
                ))
                .isGreaterThan(loadByMachine.get(
                        input.machineIds().get("FINISH")
                ));
    }

    private ScenarioInput input(String scenarioKey) {
        LearningScenarioBlueprint blueprint = catalog.blueprint(scenarioKey);
        Map<String, Long> machineIds = new LinkedHashMap<>();
        AtomicLong ids = new AtomicLong(1);
        blueprint.machines().forEach(machine ->
                machineIds.put(machine.code(), ids.getAndIncrement()));
        Map<String, Long> productIds = new LinkedHashMap<>();
        Map<String, LearningScenarioBlueprint.ProductSpec> products =
                new LinkedHashMap<>();
        blueprint.products().forEach(product -> {
            productIds.put(product.code(), ids.getAndIncrement());
            products.put(product.code(), product);
        });

        List<SchedulingOrderInput> orders = new ArrayList<>();
        for (LearningScenarioBlueprint.OrderSpec order : blueprint.orders()) {
            LearningScenarioBlueprint.ProductSpec product =
                    products.get(order.productCode());
            List<SchedulingOperationInput> operations = product.operations()
                    .stream()
                    .map(operation -> operationInput(
                            scenarioKey,
                            operation,
                            machineIds,
                            ids.getAndIncrement()
                    ))
                    .toList();
            orders.add(new SchedulingOrderInput(
                    ids.getAndIncrement(),
                    order.orderNumber(),
                    productIds.get(order.productCode()),
                    order.quantity(),
                    START.plusMinutes(order.releaseOffsetMinutes()),
                    START.plusMinutes(order.dueOffsetMinutes()),
                    order.priority(),
                    operations
            ));
        }
        return new ScenarioInput(
                Map.copyOf(machineIds),
                Map.copyOf(productIds),
                List.copyOf(orders)
        );
    }

    private SchedulingOperationInput operationInput(
            String scenarioKey,
            LearningScenarioBlueprint.OperationSpec operation,
            Map<String, Long> machineIds,
            long operationId
    ) {
        List<UnavailableInterval> unavailable = scenarioKey.equals(
                "MAINTENANCE"
        ) ? List.of(new UnavailableInterval(
                START.plusHours(2),
                START.plusHours(5)
        )) : List.of();
        List<SchedulingMachineCandidateInput> candidates =
                operation.machineCandidates().entrySet().stream()
                        .map(entry -> new SchedulingMachineCandidateInput(
                                machineIds.get(entry.getKey()),
                                entry.getValue(),
                                workingTimes(),
                                unavailable
                        ))
                        .toList();
        return new SchedulingOperationInput(
                operationId,
                machineIds.get(operation.machineCode()),
                operation.sequence(),
                operation.code(),
                operation.name(),
                operation.processingMinutes(),
                workingTimes(),
                unavailable,
                candidates
        );
    }

    private List<WeeklyWorkingTime> workingTimes() {
        return List.of(
                workingTime(DayOfWeek.MONDAY),
                workingTime(DayOfWeek.TUESDAY),
                workingTime(DayOfWeek.WEDNESDAY),
                workingTime(DayOfWeek.THURSDAY),
                workingTime(DayOfWeek.FRIDAY)
        );
    }

    private WeeklyWorkingTime workingTime(DayOfWeek day) {
        return new WeeklyWorkingTime(
                day,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
    }

    private record ScenarioInput(
            Map<String, Long> machineIds,
            Map<String, Long> productIds,
            List<SchedulingOrderInput> orders
    ) {
    }
}
