package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.product.routing.Operation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "scheduled_operation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scheduled_operation_run_order_operation",
                columnNames = {
                        "schedule_run_id",
                        "production_order_id",
                        "operation_id"
                }
        )
)
public class ScheduledOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheduled_operation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_run_id", nullable = false, updatable = false)
    private ScheduleRun scheduleRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "production_order_id",
            nullable = false,
            updatable = false
    )
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false, updatable = false)
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, updatable = false)
    private Machine machine;

    @Column(name = "operation_sequence", nullable = false, updatable = false)
    private int sequence;

    @Column(name = "start_at", nullable = false, updatable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, updatable = false)
    private OffsetDateTime endAt;

    @Column(name = "working_minutes", nullable = false, updatable = false)
    private long workingMinutes;

    @Column(name = "delayed", nullable = false, updatable = false)
    private boolean delayed;

    protected ScheduledOperation() {
    }

    private ScheduledOperation(
            ScheduleRun scheduleRun,
            ProductionOrder productionOrder,
            Operation operation,
            Machine machine,
            ScheduledTask task
    ) {
        this.scheduleRun = Objects.requireNonNull(
                scheduleRun,
                "scheduleRun must not be null"
        );
        this.productionOrder = Objects.requireNonNull(
                productionOrder,
                "productionOrder must not be null"
        );
        this.operation = Objects.requireNonNull(
                operation,
                "operation must not be null"
        );
        this.machine = Objects.requireNonNull(
                machine,
                "machine must not be null"
        );
        Objects.requireNonNull(task, "task must not be null");
        this.sequence = task.sequence();
        this.startAt = task.startAt();
        this.endAt = task.endAt();
        this.workingMinutes = task.workingMinutes();
        this.delayed = task.delayed();
    }

    static ScheduledOperation create(
            ScheduleRun scheduleRun,
            ProductionOrder productionOrder,
            Operation operation,
            Machine machine,
            ScheduledTask task
    ) {
        return new ScheduledOperation(
                scheduleRun,
                productionOrder,
                operation,
                machine,
                task
        );
    }

    public Long id() {
        return id;
    }

    public ProductionOrder productionOrder() {
        return productionOrder;
    }

    public Operation operation() {
        return operation;
    }

    public Machine machine() {
        return machine;
    }

    public int sequence() {
        return sequence;
    }

    public OffsetDateTime startAt() {
        return startAt;
    }

    public OffsetDateTime endAt() {
        return endAt;
    }

    public long workingMinutes() {
        return workingMinutes;
    }

    public boolean delayed() {
        return delayed;
    }
}
