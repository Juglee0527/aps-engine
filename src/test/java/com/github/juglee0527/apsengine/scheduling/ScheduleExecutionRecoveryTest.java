package com.github.juglee0527.apsengine.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleExecutionRecoveryTest {

    @Mock
    private ScheduleExecutionTransactionService transactionService;

    @Mock
    private ScheduleExecutionDispatcher dispatcher;

    @InjectMocks
    private ScheduleExecutionRecovery recovery;

    @Test
    void reconcilesRunningBeforeRedispatchingQueuedExecutions() {
        when(transactionService.reconcileRunning()).thenReturn(2);
        when(transactionService.findQueuedIds())
                .thenReturn(List.of(11L, 12L));

        recovery.recover();

        verify(dispatcher).dispatch(11L);
        verify(dispatcher).dispatch(12L);
    }
}
