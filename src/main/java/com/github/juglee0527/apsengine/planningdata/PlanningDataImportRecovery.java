package com.github.juglee0527.apsengine.planningdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class PlanningDataImportRecovery {

    private static final Logger log =
            LoggerFactory.getLogger(PlanningDataImportRecovery.class);

    private final PlanningDataImportTransactionService transactionService;

    PlanningDataImportRecovery(
            PlanningDataImportTransactionService transactionService
    ) {
        this.transactionService = transactionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void interruptRunningImports() {
        int interrupted = transactionService.interruptRunning();
        if (interrupted > 0) {
            log.warn(
                    "Marked {} planning data imports as interrupted",
                    interrupted
            );
        }
    }
}
