package com.github.juglee0527.apsengine.scheduling;

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

    ScheduleExecutionWorker(
            ScheduleExecutionTransactionService transactionService,
            ScheduleRunService scheduleRunService
    ) {
        this.transactionService = transactionService;
        this.scheduleRunService = scheduleRunService;
    }

    void execute(Long executionId) {
        ScheduleExecutionSnapshot execution =
                transactionService.start(executionId);
        if (execution == null) {
            return;
        }

        ScheduleRun result;
        try {
            result = calculate(execution);
        } catch (RuntimeException exception) {
            log.error(
                    "Schedule calculation failed. executionId={}",
                    executionId,
                    exception
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
            log.error(
                    "Schedule result link failed. executionId={}, "
                            + "scheduleRunId={}",
                    executionId,
                    result.id(),
                    exception
            );
        }
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
}
