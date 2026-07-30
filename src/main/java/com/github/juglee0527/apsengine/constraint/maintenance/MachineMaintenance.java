package com.github.juglee0527.apsengine.constraint.maintenance;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.github.juglee0527.apsengine.capacity.UnavailableInterval;
import com.github.juglee0527.apsengine.machine.Machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "machine_maintenance",
        indexes = @Index(
                name = "ix_machine_maintenance_machine_start",
                columnList = "machine_id,start_at"
        )
)
public class MachineMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "machine_maintenance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, updatable = false)
    private Machine machine;

    @Column(name = "start_at", nullable = false, updatable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, updatable = false)
    private OffsetDateTime endAt;

    @Column(
            name = "maintenance_reason",
            nullable = false,
            updatable = false,
            length = 200
    )
    private String reason;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected MachineMaintenance() {
    }

    private MachineMaintenance(
            Machine machine,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String reason
    ) {
        this.machine = Objects.requireNonNull(
                machine,
                "machine must not be null"
        );
        this.startAt = Objects.requireNonNull(
                startAt,
                "startAt must not be null"
        );
        this.endAt = Objects.requireNonNull(
                endAt,
                "endAt must not be null"
        );
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "정비 종료시각은 시작시각보다 이후여야 합니다."
            );
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "정비 사유는 필수입니다."
            );
        }
        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 200) {
            throw new IllegalArgumentException(
                    "정비 사유는 200자 이하여야 합니다."
            );
        }
        this.reason = normalizedReason;
        this.active = true;
    }

    public static MachineMaintenance create(
            Machine machine,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String reason
    ) {
        return new MachineMaintenance(
                machine,
                startAt,
                endAt,
                reason
        );
    }

    public Long id() {
        return id;
    }

    public Machine machine() {
        return machine;
    }

    public OffsetDateTime startAt() {
        return startAt;
    }

    public OffsetDateTime endAt() {
        return endAt;
    }

    public String reason() {
        return reason;
    }

    public boolean isActive() {
        return active;
    }

    public UnavailableInterval toUnavailableInterval() {
        return new UnavailableInterval(startAt, endAt);
    }
}
