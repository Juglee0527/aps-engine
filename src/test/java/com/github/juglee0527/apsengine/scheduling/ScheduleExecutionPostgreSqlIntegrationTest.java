package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.support.PostgreSqlContainerIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ScheduleExecutionPostgreSqlIntegrationTest
        extends PostgreSqlContainerIntegrationTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    @Autowired
    private ScheduleExecutionTransactionService transactionService;

    @Autowired
    private ScheduleExecutionRepository executionRepository;

    @Autowired
    private ScheduleRunRepository scheduleRunRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearExecutions() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE schedule_execution, "
                        + "schedule_run RESTART IDENTITY CASCADE"
        );
    }

    @Test
    void keepsOneQueueRecordForSameRequestAndRejectsConflict() {
        UUID executionKey = UUID.randomUUID();

        ScheduleExecutionQueueResult first = transactionService.queue(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        );
        ScheduleExecutionQueueResult second = transactionService.queue(
                executionKey,
                PLANNING_START,
                DispatchingRule.EDD
        );

        assertThat(first.shouldDispatch()).isTrue();
        assertThat(second.shouldDispatch()).isFalse();
        assertThat(second.executionId()).isEqualTo(first.executionId());
        assertThat(executionRepository.count()).isEqualTo(1);
        assertThatThrownBy(() -> transactionService.queue(
                executionKey,
                PLANNING_START.plusMinutes(1),
                DispatchingRule.EDD
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode())
                        .isEqualTo(
                                ErrorCode
                                        .SCHEDULE_EXECUTION_REQUEST_CONFLICT
                        )
        );
    }

    @Test
    void reconcilesRunningExecutionWithCommittedScheduleResult() {
        UUID executionKey = UUID.randomUUID();
        ScheduleExecutionQueueResult queued = transactionService.queue(
                executionKey,
                PLANNING_START,
                DispatchingRule.EXPLICIT_PRIORITY
        );
        transactionService.start(queued.executionId());
        ScheduleRun result = scheduleRunRepository.saveAndFlush(
                ScheduleRun.create(
                        executionKey,
                        new SchedulingPlan(
                                PLANNING_START,
                                PLANNING_START,
                                List.of()
                        ),
                        PLANNING_START
                )
        );

        assertThat(transactionService.reconcileRunning()).isEqualTo(1);

        ScheduleExecutionResponse response =
                transactionService.find(queued.executionId());
        assertThat(response.status())
                .isEqualTo(ScheduleExecutionStatus.COMPLETED);
        assertThat(response.resultScheduleRunId()).isEqualTo(result.id());
        assertThat(response.failureReason()).isNull();
    }

    @Test
    void marksRunningExecutionFailedWhenNoResultWasCommitted() {
        ScheduleExecutionQueueResult queued = transactionService.queue(
                UUID.randomUUID(),
                PLANNING_START,
                DispatchingRule.SPT
        );
        transactionService.start(queued.executionId());

        assertThat(transactionService.reconcileRunning()).isEqualTo(1);

        ScheduleExecutionResponse response =
                transactionService.find(queued.executionId());
        assertThat(response.status())
                .isEqualTo(ScheduleExecutionStatus.FAILED);
        assertThat(response.failureReason()).contains("재시작");
        assertThat(response.resultScheduleRunId()).isNull();
    }

    @Test
    void queuesRescheduleWithSourceRuleWhenRuleIsOmitted() {
        ScheduleRun source = scheduleRunRepository.saveAndFlush(
                ScheduleRun.create(
                        UUID.randomUUID(),
                        new SchedulingPlan(
                                PLANNING_START,
                                PLANNING_START,
                                List.of()
                        ),
                        PLANNING_START,
                        DispatchingRule.EDD,
                        ScheduleKpis.empty()
                )
        );
        OffsetDateTime frozenAt = PLANNING_START.plusHours(1);
        ScheduleExecutionQueueResult queued =
                transactionService.queueReschedule(
                        source.id(),
                        UUID.randomUUID(),
                        frozenAt,
                        null
                );

        ScheduleExecutionResponse response =
                transactionService.find(queued.executionId());

        assertThat(response.status())
                .isEqualTo(ScheduleExecutionStatus.QUEUED);
        assertThat(response.sourceScheduleRunId()).isEqualTo(source.id());
        assertThat(response.frozenAt()).isEqualTo(frozenAt);
        assertThat(response.dispatchingRule())
                .isEqualTo(DispatchingRule.EDD);
    }
}
