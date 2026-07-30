package com.github.juglee0527.apsengine.scheduling;

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

    ScheduleExecutionDispatcher(
            @Qualifier("scheduleTaskExecutor") TaskExecutor taskExecutor,
            ScheduleExecutionWorker worker,
            ScheduleExecutionTransactionService transactionService
    ) {
        this.taskExecutor = taskExecutor;
        this.worker = worker;
        this.transactionService = transactionService;
    }

    void dispatch(Long executionId) {
        try {
            taskExecutor.execute(() -> worker.execute(executionId));
        } catch (TaskRejectedException exception) {
            log.warn(
                    "Schedule execution queue rejected. executionId={}",
                    executionId,
                    exception
            );
            transactionService.fail(
                    executionId,
                    "스케줄 실행 대기열이 가득 찼습니다."
            );
        }
    }
}
