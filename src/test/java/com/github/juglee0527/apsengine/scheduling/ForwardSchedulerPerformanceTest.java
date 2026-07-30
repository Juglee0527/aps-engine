package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("performance")
class ForwardSchedulerPerformanceTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    private static final List<WeeklyWorkingTime> WORKING_TIMES =
            List.of(
                    workingTime(DayOfWeek.MONDAY),
                    workingTime(DayOfWeek.TUESDAY),
                    workingTime(DayOfWeek.WEDNESDAY),
                    workingTime(DayOfWeek.THURSDAY),
                    workingTime(DayOfWeek.FRIDAY)
            );

    private final ForwardScheduler scheduler = new ForwardScheduler();

    @Test
    void measuresSmallMediumAndLargeInputs() {
        scheduler.schedule(
                PLANNING_START,
                createOrders(new Scenario("warmup", 20, 3, 10))
        );

        List<Scenario> scenarios = List.of(
                new Scenario("small", 100, 3, 20),
                new Scenario("medium", 1_000, 5, 50),
                new Scenario("large", 5_000, 5, 100)
        );
        for (Scenario scenario : scenarios) {
            measure(scenario);
        }
        if (Boolean.getBoolean("performance.profile")) {
            Scenario profileScenario =
                    new Scenario("profile-large", 20_000, 5, 100);
            for (int iteration = 0; iteration < 10; iteration++) {
                measure(profileScenario);
            }
        }
    }

    private void measure(Scenario scenario) {
        List<SchedulingOrderInput> orders = createOrders(scenario);
        resetPeakHeapUsage();
        long heapBeforeBytes = usedHeapBytes();
        long startedAt = System.nanoTime();

        SchedulingPlan result =
                scheduler.schedule(PLANNING_START, orders);

        long elapsedNanos = System.nanoTime() - startedAt;
        long peakHeapDeltaBytes = Math.max(
                0,
                peakHeapUsedBytes() - heapBeforeBytes
        );
        assertThat(result.tasks()).hasSize(scenario.taskCount());
        System.out.printf(
                "PERF scenario=%s orders=%d operationsPerOrder=%d "
                        + "machines=%d tasks=%d elapsedMs=%.3f "
                        + "peakHeapDeltaMiB=%.3f%n",
                scenario.name(),
                scenario.orderCount(),
                scenario.operationsPerOrder(),
                scenario.machineCount(),
                scenario.taskCount(),
                elapsedNanos / 1_000_000.0,
                peakHeapDeltaBytes / 1_048_576.0
        );
    }

    private List<SchedulingOrderInput> createOrders(
            Scenario scenario
    ) {
        List<SchedulingOrderInput> orders =
                new ArrayList<>(scenario.orderCount());
        for (int orderIndex = 0;
             orderIndex < scenario.orderCount();
             orderIndex++) {
            List<SchedulingOperationInput> operations =
                    new ArrayList<>(
                            scenario.operationsPerOrder()
                    );
            for (int operationIndex = 0;
                 operationIndex < scenario.operationsPerOrder();
                 operationIndex++) {
                long operationId =
                        (long) orderIndex
                                * scenario.operationsPerOrder()
                                + operationIndex + 1;
                long machineId =
                        (orderIndex + operationIndex)
                                % scenario.machineCount() + 1L;
                operations.add(new SchedulingOperationInput(
                        operationId,
                        machineId,
                        operationIndex + 1,
                        "OP-%d".formatted(operationIndex + 1),
                        "공정 %d".formatted(operationIndex + 1),
                        5,
                        WORKING_TIMES
                ));
            }
            orders.add(new SchedulingOrderInput(
                    orderIndex + 1L,
                    "PO-%06d".formatted(orderIndex + 1),
                    orderIndex % 100 + 1L,
                    1,
                    PLANNING_START,
                    PLANNING_START.plusDays(365),
                    orderIndex % 100 + 1,
                    operations
            ));
        }
        return List.copyOf(orders);
    }

    private long usedHeapBytes() {
        return ManagementFactory.getMemoryMXBean()
                .getHeapMemoryUsage()
                .getUsed();
    }

    private void resetPeakHeapUsage() {
        for (MemoryPoolMXBean memoryPool
                : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getType() == MemoryType.HEAP) {
                memoryPool.resetPeakUsage();
            }
        }
    }

    private long peakHeapUsedBytes() {
        long peakUsedBytes = 0;
        for (MemoryPoolMXBean memoryPool
                : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getType() == MemoryType.HEAP) {
                peakUsedBytes = Math.addExact(
                        peakUsedBytes,
                        memoryPool.getPeakUsage().getUsed()
                );
            }
        }
        return peakUsedBytes;
    }

    private static WeeklyWorkingTime workingTime(
            DayOfWeek dayOfWeek
    ) {
        return new WeeklyWorkingTime(
                dayOfWeek,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );
    }

    private record Scenario(
            String name,
            int orderCount,
            int operationsPerOrder,
            int machineCount
    ) {

        private int taskCount() {
            return Math.multiplyExact(
                    orderCount,
                    operationsPerOrder
            );
        }
    }
}
