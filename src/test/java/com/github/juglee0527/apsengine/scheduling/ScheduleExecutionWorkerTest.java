package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({
        MockitoExtension.class,
        OutputCaptureExtension.class
})
class ScheduleExecutionWorkerTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    @Mock
    private ScheduleExecutionTransactionService transactionService;

    @Mock
    private ScheduleRunService scheduleRunService;

    @Mock
    private ScheduleRun scheduleRun;

    @Mock
    private ScheduleExecutionMetrics metrics;

    private ScheduleExecutionWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ScheduleExecutionWorker(
                transactionService,
                scheduleRunService,
                metrics
        );
    }

    @Test
    void calculatesAndLinksNormalScheduleResult(CapturedOutput output) {
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
        when(metrics.recordSuccess(
                any(Duration.class),
                eq(scheduleRun)
        )).thenReturn(new ScheduleExecutionCounts(2, 3, 4));

        worker.execute(31L);

        verify(transactionService).complete(31L, 41L);
        verify(metrics).recordSuccess(
                any(Duration.class),
                eq(scheduleRun)
        );
        verify(transactionService, never()).fail(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
        assertThat(output)
                .contains("event=schedule_execution_completed")
                .contains("executionId=31")
                .contains("resultScheduleRunId=41")
                .contains("outcome=success")
                .contains("orderCount=2")
                .contains("operationCount=3")
                .contains("taskCount=4")
                .doesNotContain(executionKey.toString());
    }

    @Test
    void storesExpectedApplicationFailureReason(CapturedOutput output) {
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
        verify(metrics).recordFailure(
                any(Duration.class),
                eq(ScheduleExecutionFailureStage.CALCULATION)
        );
        assertThat(output)
                .contains("event=schedule_execution_failed")
                .contains("executionId=31")
                .contains("outcome=failure")
                .contains("failureStage=calculation")
                .contains("failureType=ApplicationException")
                .doesNotContain(executionKey.toString());
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
        verify(metrics).recordFailure(
                any(Duration.class),
                eq(ScheduleExecutionFailureStage.RESULT_LINK)
        );
    }
}
