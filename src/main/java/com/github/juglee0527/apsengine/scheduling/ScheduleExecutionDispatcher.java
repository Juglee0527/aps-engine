package com.github.juglee0527.apsengine.scheduling;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

@Component
class ScheduleExecutionDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(ScheduleExecutionDispatcher.class);

    private final TaskExecutor taskExecutor;
    private final ScheduleExecutionWorker worker;
    private final ScheduleExecutionTransactionService transactionService;
    private final ScheduleExecutionMetrics metrics;

    ScheduleExecutionDispatcher(
            @Qualifier("scheduleTaskExecutor") TaskExecutor taskExecutor,
            ScheduleExecutionWorker worker,
            ScheduleExecutionTransactionService transactionService,
            ScheduleExecutionMetrics metrics
    ) {
        this.taskExecutor = taskExecutor;
        this.worker = worker;
        this.transactionService = transactionService;
        this.metrics = metrics;
    }

    void dispatch(Long executionId) {
        try {
            taskExecutor.execute(() -> worker.execute(executionId));
        } catch (TaskRejectedException exception) {
            metrics.recordFailure(
                    Duration.ZERO,
                    ScheduleExecutionFailureStage.QUEUE
            );
            log.warn(
                    "event=schedule_execution_failed executionId={} "
                            + "outcome=failure failureStage=queue "
                            + "failureType={}",
                    executionId,
                    exception.getClass().getSimpleName()
            );
            transactionService.fail(
                    executionId,
                    "스케줄 실행 대기열이 가득 찼습니다."
            );
        }
    }
}
