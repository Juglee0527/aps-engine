package com.github.juglee0527.apsengine.product.routing;

import java.util.Objects;

import com.github.juglee0527.apsengine.machine.Machine;

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
        name = "operation_machine_candidate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_operation_machine_candidate",
                columnNames = {"operation_id", "machine_id"}
        )
)
public class OperationMachineCandidate {

    static final int MAX_PRIORITY = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_machine_candidate_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false, updatable = false)
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, updatable = false)
    private Machine machine;

    @Column(name = "candidate_priority", nullable = false, updatable = false)
    private int priority;

    protected OperationMachineCandidate() {
    }

    private OperationMachineCandidate(
            Operation operation,
            Machine machine,
            int priority
    ) {
        this.operation = Objects.requireNonNull(
                operation,
                "operation must not be null"
        );
        this.machine = Objects.requireNonNull(
                machine,
                "machine must not be null"
        );
        if (priority < 1 || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "후보 설비 우선순위는 1 이상 1000 이하여야 합니다."
            );
        }
        this.priority = priority;
    }

    static OperationMachineCandidate create(
            Operation operation,
            Machine machine,
            int priority
    ) {
        return new OperationMachineCandidate(operation, machine, priority);
    }

    public Long id() {
        return id;
    }

    public Operation operation() {
        return operation;
    }

    public Machine machine() {
        return machine;
    }

    public int priority() {
        return priority;
    }
}
