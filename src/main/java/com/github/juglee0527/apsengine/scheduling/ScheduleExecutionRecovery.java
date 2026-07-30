package com.github.juglee0527.apsengine.scheduling;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class ScheduleExecutionRecovery {

    private static final Logger log =
            LoggerFactory.getLogger(ScheduleExecutionRecovery.class);

    private final ScheduleExecutionTransactionService transactionService;
    private final ScheduleExecutionDispatcher dispatcher;

    ScheduleExecutionRecovery(
            ScheduleExecutionTransactionService transactionService,
            ScheduleExecutionDispatcher dispatcher
    ) {
        this.transactionService = transactionService;
        this.dispatcher = dispatcher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        int reconciled = transactionService.reconcileRunning();
        List<Long> queuedIds = transactionService.findQueuedIds();
        for (Long queuedId : queuedIds) {
            dispatcher.dispatch(queuedId);
        }
        if (reconciled > 0 || !queuedIds.isEmpty()) {
            log.info(
                    "event=schedule_execution_recovery reconciled={} "
                            + "redispatched={}",
                    reconciled,
                    queuedIds.size()
            );
        }
    }
}
