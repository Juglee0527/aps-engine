package com.github.juglee0527.apsengine.scheduling;

import java.time.Duration;

import com.github.juglee0527.apsengine.common.error.ApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class ScheduleExecutionWorker {

    private static final Logger log =
            LoggerFactory.getLogger(ScheduleExecutionWorker.class);

    private final ScheduleExecutionTransactionService transactionService;
    private final ScheduleRunService scheduleRunService;
    private final ScheduleExecutionMetrics metrics;

    ScheduleExecutionWorker(
            ScheduleExecutionTransactionService transactionService,
            ScheduleRunService scheduleRunService,
            ScheduleExecutionMetrics metrics
    ) {
        this.transactionService = transactionService;
        this.scheduleRunService = scheduleRunService;
        this.metrics = metrics;
    }

    void execute(Long executionId) {
        long startedNanos = System.nanoTime();
        ScheduleExecutionSnapshot execution;
        try {
            execution = transactionService.start(executionId);
        } catch (RuntimeException exception) {
            Duration duration = elapsed(startedNanos);
            metrics.recordFailure(
                    duration,
                    ScheduleExecutionFailureStage.START
            );
            log.warn(
                    "event=schedule_execution_failed executionId={} "
                            + "outcome=failure failureStage=start "
                            + "failureType={} durationMs={}",
                    executionId,
                    exception.getClass().getSimpleName(),
                    duration.toMillis()
            );
            return;
        }
        if (execution == null) {
            return;
        }

        ScheduleRun result;
        try {
            result = calculate(execution);
        } catch (RuntimeException exception) {
            Duration duration = elapsed(startedNanos);
            metrics.recordFailure(
                    duration,
                    ScheduleExecutionFailureStage.CALCULATION
            );
            log.warn(
                    "event=schedule_execution_failed executionId={} "
                            + "outcome=failure failureStage=calculation "
                            + "failureType={} durationMs={}",
                    executionId,
                    exception.getClass().getSimpleName(),
                    duration.toMillis()
            );
            transactionService.fail(
                    executionId,
                    failureReason(exception)
            );
            return;
        }

        try {
            transactionService.complete(executionId, result.id());
        } catch (RuntimeException exception) {
            Duration duration = elapsed(startedNanos);
            metrics.recordFailure(
                    duration,
                    ScheduleExecutionFailureStage.RESULT_LINK
            );
            log.warn(
                    "event=schedule_execution_failed executionId={} "
                            + "resultScheduleRunId={} outcome=failure "
                            + "failureStage=result_link failureType={} "
                            + "durationMs={}",
                    executionId,
                    result.id(),
                    exception.getClass().getSimpleName(),
                    duration.toMillis()
            );
            return;
        }

        Duration duration = elapsed(startedNanos);
        ScheduleExecutionCounts counts =
                metrics.recordSuccess(duration, result);
        log.info(
                "event=schedule_execution_completed executionId={} "
                        + "resultScheduleRunId={} outcome=success "
                        + "durationMs={} orderCount={} operationCount={} "
                        + "taskCount={}",
                executionId,
                result.id(),
                duration.toMillis(),
                counts.orderCount(),
                counts.operationCount(),
                counts.taskCount()
        );
    }

    private ScheduleRun calculate(ScheduleExecutionSnapshot execution) {
        if (execution.sourceScheduleRunId() == null) {
            return scheduleRunService.execute(
                    execution.executionKey(),
                    execution.planningStart(),
                    execution.dispatchingRule()
            );
        }
        return scheduleRunService.reschedule(
                execution.sourceScheduleRunId(),
                execution.executionKey(),
                execution.frozenAt(),
                execution.dispatchingRule()
        );
    }

    private String failureReason(RuntimeException exception) {
        if (exception instanceof ApplicationException) {
            return exception.getMessage();
        }
        return "스케줄 계산 중 예상하지 못한 오류가 발생했습니다.";
    }

    private Duration elapsed(long startedNanos) {
        long elapsedNanos = Math.max(
                0L,
                System.nanoTime() - startedNanos
        );
        return Duration.ofNanos(elapsedNanos);
    }
}
