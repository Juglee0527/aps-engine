package com.github.juglee0527.apsengine.scheduling;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

@Component
class ScheduleExecutionMetrics {

    static final String DURATION_METRIC =
            "aps.schedule.execution.duration";
    static final String FAILURE_METRIC =
            "aps.schedule.execution.failures";
    static final String ORDER_COUNT_METRIC =
            "aps.schedule.execution.input.orders";
    static final String OPERATION_COUNT_METRIC =
            "aps.schedule.execution.input.operations";
    static final String TASK_COUNT_METRIC =
            "aps.schedule.execution.output.tasks";

    private final Timer successDuration;
    private final Timer failureDuration;
    private final Map<ScheduleExecutionFailureStage, Counter>
            failureCounters;
    private final DistributionSummary inputOrders;
    private final DistributionSummary inputOperations;
    private final DistributionSummary outputTasks;

    ScheduleExecutionMetrics(MeterRegistry meterRegistry) {
        successDuration = Timer.builder(DURATION_METRIC)
                .description("비동기 스케줄 실행 처리시간")
                .tag("outcome", "success")
                .register(meterRegistry);
        failureDuration = Timer.builder(DURATION_METRIC)
                .description("비동기 스케줄 실행 처리시간")
                .tag("outcome", "failure")
                .register(meterRegistry);
        failureCounters =
                new EnumMap<>(ScheduleExecutionFailureStage.class);
        for (ScheduleExecutionFailureStage stage
                : ScheduleExecutionFailureStage.values()) {
            failureCounters.put(
                    stage,
                    Counter.builder(FAILURE_METRIC)
                            .description("비동기 스케줄 실행 실패 수")
                            .tag("stage", stage.metricTag())
                            .register(meterRegistry)
            );
        }
        inputOrders = DistributionSummary
                .builder(ORDER_COUNT_METRIC)
                .description("완료된 실행의 입력 생산오더 수")
                .baseUnit("orders")
                .register(meterRegistry);
        inputOperations = DistributionSummary
                .builder(OPERATION_COUNT_METRIC)
                .description("완료된 실행의 입력 공정 정의 수")
                .baseUnit("operations")
                .register(meterRegistry);
        outputTasks = DistributionSummary
                .builder(TASK_COUNT_METRIC)
                .description("완료된 실행의 생성 작업 수")
                .baseUnit("tasks")
                .register(meterRegistry);
    }

    ScheduleExecutionCounts recordSuccess(
            Duration duration,
            ScheduleRun scheduleRun
    ) {
        Objects.requireNonNull(scheduleRun, "scheduleRun must not be null");
        ScheduleExecutionCounts counts = counts(scheduleRun);
        successDuration.record(normalize(duration));
        inputOrders.record(counts.orderCount());
        inputOperations.record(counts.operationCount());
        outputTasks.record(counts.taskCount());
        return counts;
    }

    void recordFailure(
            Duration duration,
            ScheduleExecutionFailureStage stage
    ) {
        failureDuration.record(normalize(duration));
        failureCounters.get(Objects.requireNonNull(
                stage,
                "stage must not be null"
        )).increment();
    }

    private ScheduleExecutionCounts counts(ScheduleRun scheduleRun) {
        List<ScheduledOperation> scheduledOperations =
                scheduleRun.scheduledOperations();
        long orderCount = scheduledOperations
                .stream()
                .map(operation -> operation.productionOrder().id())
                .distinct()
                .count();
        long operationCount = scheduledOperations
                .stream()
                .map(operation -> operation.operation().id())
                .distinct()
                .count();
        return new ScheduleExecutionCounts(
                orderCount,
                operationCount,
                scheduledOperations.size()
        );
    }

    private Duration normalize(Duration duration) {
        Duration value = Objects.requireNonNull(
                duration,
                "duration must not be null"
        );
        return value.isNegative() ? Duration.ZERO : value;
    }
}
