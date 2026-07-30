package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.product.routing.Operation;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

class ScheduleExecutionMetricsTest {

    @Test
    void recordsSuccessfulExecutionCountsWithoutBusinessIdentifiers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ScheduleExecutionMetrics metrics =
                new ScheduleExecutionMetrics(registry);
        ScheduleRun scheduleRun = scheduleRunWithThreeTasks();

        ScheduleExecutionCounts counts = metrics.recordSuccess(
                Duration.ofMillis(125),
                scheduleRun
        );

        assertThat(counts).isEqualTo(
                new ScheduleExecutionCounts(2, 2, 3)
        );
        assertThat(registry.get(
                ScheduleExecutionMetrics.DURATION_METRIC
        ).tag("outcome", "success").timer().count()).isEqualTo(1);
        assertThat(registry.get(
                ScheduleExecutionMetrics.DURATION_METRIC
        ).tag("outcome", "success").timer().totalTime(
                TimeUnit.MILLISECONDS
        )).isEqualTo(125);
        assertThat(registry.get(
                ScheduleExecutionMetrics.ORDER_COUNT_METRIC
        ).summary().totalAmount()).isEqualTo(2);
        assertThat(registry.get(
                ScheduleExecutionMetrics.OPERATION_COUNT_METRIC
        ).summary().totalAmount()).isEqualTo(2);
        assertThat(registry.get(
                ScheduleExecutionMetrics.TASK_COUNT_METRIC
        ).summary().totalAmount()).isEqualTo(3);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(
                        meter.getId().getTags()
                ).allSatisfy(tag -> assertThat(tag.getKey())
                        .isIn("outcome", "stage")));
    }

    @Test
    void recordsFailureByFixedLifecycleStage() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ScheduleExecutionMetrics metrics =
                new ScheduleExecutionMetrics(registry);

        metrics.recordFailure(
                Duration.ofMillis(25),
                ScheduleExecutionFailureStage.CALCULATION
        );

        assertThat(registry.get(
                ScheduleExecutionMetrics.FAILURE_METRIC
        ).tag("stage", "calculation").counter().count())
                .isEqualTo(1);
        assertThat(registry.get(
                ScheduleExecutionMetrics.DURATION_METRIC
        ).tag("outcome", "failure").timer().totalTime(
                TimeUnit.MILLISECONDS
        )).isEqualTo(25);
    }

    private ScheduleRun scheduleRunWithThreeTasks() {
        ProductionOrder firstOrder = productionOrder(11L);
        ProductionOrder secondOrder = productionOrder(12L);
        Operation cutting = operation(21L);
        Operation assembly = operation(22L);

        ScheduleRun scheduleRun = mock(ScheduleRun.class);
        List<ScheduledOperation> scheduledOperations = List.of(
                scheduledOperation(firstOrder, cutting),
                scheduledOperation(firstOrder, assembly),
                scheduledOperation(secondOrder, cutting)
        );
        when(scheduleRun.scheduledOperations())
                .thenReturn(scheduledOperations);
        return scheduleRun;
    }

    private ScheduledOperation scheduledOperation(
            ProductionOrder productionOrder,
            Operation operation
    ) {
        ScheduledOperation scheduledOperation =
                mock(ScheduledOperation.class);
        when(scheduledOperation.productionOrder())
                .thenReturn(productionOrder);
        when(scheduledOperation.operation()).thenReturn(operation);
        return scheduledOperation;
    }

    private ProductionOrder productionOrder(long id) {
        ProductionOrder productionOrder = mock(ProductionOrder.class);
        when(productionOrder.id()).thenReturn(id);
        return productionOrder;
    }

    private Operation operation(long id) {
        Operation operation = mock(Operation.class);
        when(operation.id()).thenReturn(id);
        return operation;
    }
}
