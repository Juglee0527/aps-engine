package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "schedule_execution",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_schedule_execution_key",
                        columnNames = "execution_key"
                ),
                @UniqueConstraint(
                        name = "uk_schedule_execution_result",
                        columnNames = "result_schedule_run_id"
                )
        }
)
public class ScheduleExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_execution_id")
    private Long id;

    @Column(name = "execution_key", nullable = false, updatable = false)
    private UUID executionKey;

    @Column(name = "planning_start", nullable = false, updatable = false)
    private OffsetDateTime planningStart;

    @Column(
            name = "planning_offset_seconds",
            nullable = false,
            updatable = false
    )
    private int planningOffsetSeconds;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "dispatching_rule",
            nullable = false,
            updatable = false,
            length = 30
    )
    private DispatchingRule dispatchingRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_schedule_run_id", updatable = false)
    private ScheduleRun sourceScheduleRun;

    @Column(name = "frozen_at", updatable = false)
    private OffsetDateTime frozenAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_schedule_run_id")
    private ScheduleRun resultScheduleRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleExecutionStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected ScheduleExecution() {
    }

    private ScheduleExecution(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule,
            ScheduleRun sourceScheduleRun,
            OffsetDateTime frozenAt,
            OffsetDateTime createdAt
    ) {
        this.executionKey = Objects.requireNonNull(
                executionKey,
                "executionKey must not be null"
        );
        this.planningStart = Objects.requireNonNull(
                planningStart,
                "planningStart must not be null"
        );
        this.planningOffsetSeconds =
                planningStart.getOffset().getTotalSeconds();
        this.dispatchingRule = Objects.requireNonNull(
                dispatchingRule,
                "dispatchingRule must not be null"
        );
        if ((sourceScheduleRun == null) != (frozenAt == null)) {
            throw new IllegalArgumentException(
                    "원본 실행과 동결 기준시각은 함께 지정해야 합니다."
            );
        }
        if (frozenAt != null && frozenAt.isBefore(planningStart)) {
            throw new IllegalArgumentException(
                    "동결 기준시각은 계획 시작시각보다 이전일 수 없습니다."
            );
        }
        this.sourceScheduleRun = sourceScheduleRun;
        this.frozenAt = frozenAt;
        this.status = ScheduleExecutionStatus.QUEUED;
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );
    }

    public static ScheduleExecution queue(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule,
            OffsetDateTime createdAt
    ) {
        return new ScheduleExecution(
                executionKey,
                planningStart,
                dispatchingRule,
                null,
                null,
                createdAt
        );
    }

    public static ScheduleExecution queueReschedule(
            UUID executionKey,
            ScheduleRun sourceScheduleRun,
            OffsetDateTime frozenAt,
            DispatchingRule dispatchingRule,
            OffsetDateTime createdAt
    ) {
        Objects.requireNonNull(
                sourceScheduleRun,
                "sourceScheduleRun must not be null"
        );
        return new ScheduleExecution(
                executionKey,
                sourceScheduleRun.planningStart(),
                dispatchingRule,
                sourceScheduleRun,
                frozenAt,
                createdAt
        );
    }

    public void start(OffsetDateTime now) {
        requireStatus(ScheduleExecutionStatus.QUEUED);
        status = ScheduleExecutionStatus.RUNNING;
        startedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void complete(
            ScheduleRun scheduleRun,
            OffsetDateTime now
    ) {
        requireStatus(ScheduleExecutionStatus.RUNNING);
        ScheduleRun result = Objects.requireNonNull(
                scheduleRun,
                "scheduleRun must not be null"
        );
        if (!executionKey.equals(result.executionKey())) {
            throw new IllegalArgumentException(
                    "실행 요청 키와 결과 키가 일치하지 않습니다."
            );
        }
        resultScheduleRun = result;
        status = ScheduleExecutionStatus.COMPLETED;
        failureReason = null;
        completedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void fail(String reason, OffsetDateTime now) {
        if (status != ScheduleExecutionStatus.QUEUED
                && status != ScheduleExecutionStatus.RUNNING) {
            throw new IllegalStateException(
                    "현재 실행 상태에서는 실패로 변경할 수 없습니다: "
                            + status
            );
        }
        status = ScheduleExecutionStatus.FAILED;
        failureReason = normalizeFailureReason(reason);
        completedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public boolean matches(
            OffsetDateTime requestedPlanningStart,
            DispatchingRule requestedRule,
            Long requestedSourceScheduleRunId,
            OffsetDateTime requestedFrozenAt
    ) {
        return planningStart().equals(requestedPlanningStart)
                && dispatchingRule == requestedRule
                && Objects.equals(
                        sourceScheduleRunId(),
                        requestedSourceScheduleRunId
                )
                && sameInstant(frozenAt, requestedFrozenAt);
    }

    public Long id() {
        return id;
    }

    public UUID executionKey() {
        return executionKey;
    }

    public OffsetDateTime planningStart() {
        return planningStart.withOffsetSameInstant(
                ZoneOffset.ofTotalSeconds(planningOffsetSeconds)
        );
    }

    public int planningOffsetSeconds() {
        return planningOffsetSeconds;
    }

    public DispatchingRule dispatchingRule() {
        return dispatchingRule;
    }

    public Long sourceScheduleRunId() {
        return sourceScheduleRun == null
                ? null
                : sourceScheduleRun.id();
    }

    public OffsetDateTime frozenAt() {
        return frozenAt;
    }

    public Long resultScheduleRunId() {
        return resultScheduleRun == null
                ? null
                : resultScheduleRun.id();
    }

    public ScheduleExecutionStatus status() {
        return status;
    }

    public String failureReason() {
        return failureReason;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime startedAt() {
        return startedAt;
    }

    public OffsetDateTime completedAt() {
        return completedAt;
    }

    private void requireStatus(ScheduleExecutionStatus required) {
        if (status != required) {
            throw new IllegalStateException(
                    "실행 상태를 %s에서 변경할 수 없습니다."
                            .formatted(status)
            );
        }
    }

    private String normalizeFailureReason(String reason) {
        String normalized = reason == null || reason.isBlank()
                ? "스케줄 계산에 실패했습니다."
                : reason.trim();
        return normalized.length() <= 500
                ? normalized
                : normalized.substring(0, 500);
    }

    private boolean sameInstant(
            OffsetDateTime left,
            OffsetDateTime right
    ) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.isEqual(right);
    }
}
