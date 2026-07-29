package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        this.status = ScheduleRunStatus.COMPLETED;
    }

    public static ScheduleRun create(
            UUID executionKey,
            SchedulingPlan plan,
            OffsetDateTime createdAt
    ) {
        Objects.requireNonNull(plan, "plan must not be null");
        return new ScheduleRun(
                executionKey,
                plan.planningStart(),
                plan.schedulingEnd(),
                createdAt
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
        return planningStart;
    }

    public OffsetDateTime schedulingEnd() {
        return schedulingEnd;
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

    public List<ScheduledOperation> scheduledOperations() {
        return Collections.unmodifiableList(scheduledOperations);
    }
}
