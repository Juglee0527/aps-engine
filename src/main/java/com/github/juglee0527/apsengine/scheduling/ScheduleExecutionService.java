package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ScheduleExecutionService {

    private final ScheduleExecutionTransactionService transactionService;
    private final ScheduleExecutionDispatcher dispatcher;

    public ScheduleExecutionService(
            ScheduleExecutionTransactionService transactionService,
            ScheduleExecutionDispatcher dispatcher
    ) {
        this.transactionService = transactionService;
        this.dispatcher = dispatcher;
    }

    public ScheduleExecutionResponse submit(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule
    ) {
        ScheduleExecutionQueueResult queued;
        try {
            queued = transactionService.queue(
                    executionKey,
                    planningStart,
                    dispatchingRule
            );
        } catch (DataIntegrityViolationException exception) {
            return transactionService.findMatching(
                    executionKey,
                    planningStart,
                    dispatchingRule
            );
        }
        if (queued.shouldDispatch()) {
            dispatcher.dispatch(queued.executionId());
        }
        return transactionService.find(queued.executionId());
    }

    public ScheduleExecutionResponse submitReschedule(
            long sourceScheduleRunId,
            UUID executionKey,
            OffsetDateTime frozenAt,
            DispatchingRule requestedRule
    ) {
        ScheduleExecutionQueueResult queued;
        try {
            queued = transactionService.queueReschedule(
                    sourceScheduleRunId,
                    executionKey,
                    frozenAt,
                    requestedRule
            );
        } catch (DataIntegrityViolationException exception) {
            return transactionService.findMatchingReschedule(
                    sourceScheduleRunId,
                    executionKey,
                    frozenAt,
                    requestedRule
            );
        }
        if (queued.shouldDispatch()) {
            dispatcher.dispatch(queued.executionId());
        }
        return transactionService.find(queued.executionId());
    }

    public ScheduleExecutionResponse find(Long executionId) {
        return transactionService.find(executionId);
    }

    public List<ScheduleExecutionResponse> findRecent(int limit) {
        return transactionService.findRecent(limit);
    }
}
