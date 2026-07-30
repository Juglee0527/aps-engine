package com.github.juglee0527.apsengine.scheduling;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.product.routing.Operation;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "schedule_run",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_run_execution_key",
                columnNames = "execution_key"
        )
)
public class ScheduleRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_run_id")
    private Long id;

    @Column(name = "execution_key", nullable = false, updatable = false)
    private UUID executionKey;

    @Column(name = "planning_start", nullable = false, updatable = false)
    private OffsetDateTime planningStart;

    @Column(name = "scheduling_end", nullable = false, updatable = false)
    private OffsetDateTime schedulingEnd;

    @Column(
            name = "planning_offset_seconds",
            nullable = false,
            updatable = false
    )
    private int planningOffsetSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_schedule_run_id", updatable = false)
    private ScheduleRun sourceScheduleRun;

    @Column(name = "frozen_at", updatable = false)
    private OffsetDateTime frozenAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "dispatching_rule",
            nullable = false,
            updatable = false,
            length = 30
    )
    private DispatchingRule dispatchingRule;

    @Column(
            name = "total_tardiness_minutes",
            nullable = false,
            updatable = false
    )
    private long totalTardinessMinutes;

    @Column(
            name = "delayed_order_count",
            nullable = false,
            updatable = false
    )
    private int delayedOrderCount;

    @Column(
            name = "makespan_minutes",
            nullable = false,
            updatable = false
    )
    private long makespanMinutes;

    @Column(
            name = "machine_utilization_percent",
            nullable = false,
            updatable = false,
            precision = 7,
            scale = 2
    )
    private BigDecimal machineUtilizationPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleRunStatus status;

    @OneToMany(
            mappedBy = "scheduleRun",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("startAt ASC, id ASC")
    private List<ScheduledOperation> scheduledOperations =
            new ArrayList<>();

    protected ScheduleRun() {
    }

    private ScheduleRun(
            UUID executionKey,
            OffsetDateTime planningStart,
            OffsetDateTime schedulingEnd,
            OffsetDateTime createdAt,
            DispatchingRule dispatchingRule,
            ScheduleKpis kpis,
            ScheduleRun sourceScheduleRun,
            OffsetDateTime frozenAt
    ) {
        this.executionKey = Objects.requireNonNull(
                executionKey,
                "executionKey must not be null"
        );
        this.planningStart = Objects.requireNonNull(
                planningStart,
                "planningStart must not be null"
        );
        this.schedulingEnd = Objects.requireNonNull(
                schedulingEnd,
                "schedulingEnd must not be null"
        );
        if (schedulingEnd.isBefore(planningStart)) {
            throw new IllegalArgumentException(
                    "스케줄 종료시각은 계획 시작시각보다 이전일 수 없습니다."
            );
        }
        this.planningOffsetSeconds =
                planningStart.getOffset().getTotalSeconds();
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );
        this.dispatchingRule = Objects.requireNonNull(
                dispatchingRule,
                "dispatchingRule must not be null"
        );
        Objects.requireNonNull(kpis, "kpis must not be null");
        this.totalTardinessMinutes = kpis.totalTardinessMinutes();
        this.delayedOrderCount = kpis.delayedOrderCount();
        this.makespanMinutes = kpis.makespanMinutes();
        this.machineUtilizationPercent =
                kpis.machineUtilizationPercent();
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
        this.status = ScheduleRunStatus.COMPLETED;
    }

    public static ScheduleRun create(
            UUID executionKey,
            SchedulingPlan plan,
            OffsetDateTime createdAt
    ) {
        return create(
                executionKey,
                plan,
                createdAt,
                DispatchingRule.EXPLICIT_PRIORITY,
                ScheduleKpis.empty()
        );
    }

    public static ScheduleRun create(
            UUID executionKey,
            SchedulingPlan plan,
            OffsetDateTime createdAt,
            DispatchingRule dispatchingRule,
            ScheduleKpis kpis
    ) {
        Objects.requireNonNull(plan, "plan must not be null");
        return new ScheduleRun(
                executionKey,
                plan.planningStart(),
                plan.schedulingEnd(),
                createdAt,
                dispatchingRule,
                kpis,
                null,
                null
        );
    }

    public static ScheduleRun createRescheduled(
            UUID executionKey,
            SchedulingPlan plan,
            OffsetDateTime createdAt,
            DispatchingRule dispatchingRule,
            ScheduleKpis kpis,
            ScheduleRun sourceScheduleRun,
            OffsetDateTime frozenAt
    ) {
        Objects.requireNonNull(plan, "plan must not be null");
        return new ScheduleRun(
                executionKey,
                plan.planningStart(),
                plan.schedulingEnd(),
                createdAt,
                dispatchingRule,
                kpis,
                Objects.requireNonNull(
                        sourceScheduleRun,
                        "sourceScheduleRun must not be null"
                ),
                Objects.requireNonNull(
                        frozenAt,
                        "frozenAt must not be null"
                )
        );
    }

    public void addScheduledOperation(
            ProductionOrder productionOrder,
            Operation operation,
            Machine machine,
            ScheduledTask task
    ) {
        scheduledOperations.add(ScheduledOperation.create(
                this,
                productionOrder,
                operation,
                machine,
                task
        ));
    }

    public Long id() {
        return id;
    }

    public UUID executionKey() {
        return executionKey;
    }

    public OffsetDateTime planningStart() {
        return withPlanningOffset(planningStart);
    }

    public OffsetDateTime schedulingEnd() {
        return withPlanningOffset(schedulingEnd);
    }

    public int planningOffsetSeconds() {
        return planningOffsetSeconds;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public ScheduleRunStatus status() {
        return status;
    }

    public Long sourceScheduleRunId() {
        return sourceScheduleRun == null
                ? null
                : sourceScheduleRun.id();
    }

    public OffsetDateTime frozenAt() {
        return frozenAt;
    }

    public DispatchingRule dispatchingRule() {
        return dispatchingRule;
    }

    public long totalTardinessMinutes() {
        return totalTardinessMinutes;
    }

    public int delayedOrderCount() {
        return delayedOrderCount;
    }

    public long makespanMinutes() {
        return makespanMinutes;
    }

    public BigDecimal machineUtilizationPercent() {
        return machineUtilizationPercent;
    }

    public List<ScheduledOperation> scheduledOperations() {
        return Collections.unmodifiableList(scheduledOperations);
    }

    private OffsetDateTime withPlanningOffset(OffsetDateTime value) {
        return value.withOffsetSameInstant(
                ZoneOffset.ofTotalSeconds(planningOffsetSeconds)
        );
    }
}
