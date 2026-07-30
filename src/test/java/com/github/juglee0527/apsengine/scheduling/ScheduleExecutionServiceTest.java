package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleExecutionServiceTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    @Mock
    private ScheduleExecutionTransactionService transactionService;

    @Mock
    private ScheduleExecutionDispatcher dispatcher;

    private ScheduleExecutionService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleExecutionService(
                transactionService,
                dispatcher
        );
    }

    @Test
    void dispatchesOnlyNewExecution() {
        UUID executionKey = UUID.randomUUID();
        ScheduleExecutionResponse queued = response(
                executionKey,
                ScheduleExecutionStatus.QUEUED
        );
        when(transactionService.queue(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        )).thenReturn(new ScheduleExecutionQueueResult(31L, true));
        when(transactionService.find(31L)).thenReturn(queued);

        ScheduleExecutionResponse result = service.submit(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        );

        assertThat(result).isSameAs(queued);
        verify(dispatcher).dispatch(31L);
    }

    @Test
    void doesNotDispatchIdempotentDuplicateAgain() {
        UUID executionKey = UUID.randomUUID();
        when(transactionService.queue(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        )).thenReturn(new ScheduleExecutionQueueResult(31L, false));
        when(transactionService.find(31L)).thenReturn(response(
                executionKey,
                ScheduleExecutionStatus.RUNNING
        ));

        ScheduleExecutionResponse result = service.submit(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        );

        assertThat(result.status())
                .isEqualTo(ScheduleExecutionStatus.RUNNING);
        verify(dispatcher, never()).dispatch(31L);
    }

    @Test
    void delegatesRecentHistoryLimit() {
        when(transactionService.findRecent(5)).thenReturn(List.of());

        assertThat(service.findRecent(5)).isEmpty();

        verify(transactionService).findRecent(5);
    }

    private ScheduleExecutionResponse response(
            UUID executionKey,
            ScheduleExecutionStatus status
    ) {
        return new ScheduleExecutionResponse(
                31L,
                executionKey,
                status,
                PLANNING_START,
                PLANNING_START.getOffset().getTotalSeconds(),
                DispatchingRule.EDD,
                null,
                null,
                null,
                null,
                PLANNING_START,
                status == ScheduleExecutionStatus.QUEUED
                        ? null
                        : PLANNING_START,
                null
        );
    }
}
