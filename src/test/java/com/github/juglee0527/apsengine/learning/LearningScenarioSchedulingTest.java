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
import com.github.juglee0527.apsengine.scheduling.ForwardScheduler;
import com.github.juglee0527.apsengine.scheduling.EarliestDueDateRule;
import com.github.juglee0527.apsengine.scheduling.ScheduleKpiCalculator;
import com.github.juglee0527.apsengine.scheduling.ScheduleKpis;
import com.github.juglee0527.apsengine.scheduling.SchedulingOperationInput;
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
                    .map(operation -> new SchedulingOperationInput(
                            ids.getAndIncrement(),
                            machineIds.get(operation.machineCode()),
                            operation.sequence(),
                            operation.code(),
                            operation.name(),
                            operation.processingMinutes(),
                            workingTimes()
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
                List.copyOf(orders)
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
            List<SchedulingOrderInput> orders
    ) {
    }
}
