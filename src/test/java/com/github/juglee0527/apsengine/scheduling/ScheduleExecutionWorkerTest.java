package com.github.juglee0527.apsengine.scheduling;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleExecutionWorkerTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    @Mock
    private ScheduleExecutionTransactionService transactionService;

    @Mock
    private ScheduleRunService scheduleRunService;

    @Mock
    private ScheduleRun scheduleRun;

    private ScheduleExecutionWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ScheduleExecutionWorker(
                transactionService,
                scheduleRunService
        );
    }

    @Test
    void calculatesAndLinksNormalScheduleResult() {
        UUID executionKey = UUID.randomUUID();
        ScheduleExecutionSnapshot snapshot = new ScheduleExecutionSnapshot(
                31L,
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD,
                null,
                null
        );
        when(transactionService.start(31L)).thenReturn(snapshot);
        when(scheduleRunService.execute(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        )).thenReturn(scheduleRun);
        when(scheduleRun.id()).thenReturn(41L);

        worker.execute(31L);

        verify(transactionService).complete(31L, 41L);
        verify(transactionService, never()).fail(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void storesExpectedApplicationFailureReason() {
        UUID executionKey = UUID.randomUUID();
        ScheduleExecutionSnapshot snapshot = new ScheduleExecutionSnapshot(
                31L,
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD,
                null,
                null
        );
        when(transactionService.start(31L)).thenReturn(snapshot);
        when(scheduleRunService.execute(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        )).thenThrow(new ApplicationException(
                ErrorCode.CONFIRMED_PRODUCTION_ORDER_REQUIRED
        ));

        worker.execute(31L);

        verify(transactionService).fail(
                31L,
                ErrorCode.CONFIRMED_PRODUCTION_ORDER_REQUIRED
                        .defaultMessage()
        );
        verify(transactionService, never()).complete(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    @Test
    void leavesRunningForStartupReconciliationWhenLinkFails() {
        UUID executionKey = UUID.randomUUID();
        ScheduleExecutionSnapshot snapshot = new ScheduleExecutionSnapshot(
                31L,
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD,
                null,
                null
        );
        when(transactionService.start(31L)).thenReturn(snapshot);
        when(scheduleRunService.execute(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        )).thenReturn(scheduleRun);
        when(scheduleRun.id()).thenReturn(41L);
        org.mockito.Mockito.doThrow(new RuntimeException("db unavailable"))
                .when(transactionService)
                .complete(31L, 41L);

        worker.execute(31L);

        verify(transactionService, never()).fail(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
