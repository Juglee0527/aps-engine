package com.github.juglee0527.apsengine.product.routing;

import java.util.Objects;

import com.github.juglee0527.apsengine.common.domain.BusinessCodeNormalizer;
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
        name = "operation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_operation_routing_sequence",
                        columnNames = {"routing_id", "operation_sequence"}
                ),
                @UniqueConstraint(
                        name = "uk_operation_routing_code",
                        columnNames = {"routing_id", "operation_code"}
                )
        }
)
public class Operation {

    static final int MAX_CODE_LENGTH = 50;
    static final int MAX_NAME_LENGTH = 100;
    static final int MAX_PROCESSING_TIME_MINUTES = 10_080;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routing_id", nullable = false, updatable = false)
    private Routing routing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, updatable = false)
    private Machine machine;

    @Column(name = "operation_sequence", nullable = false, updatable = false)
    private int sequence;

    @Column(
            name = "operation_code",
            nullable = false,
            updatable = false,
            length = MAX_CODE_LENGTH
    )
    private String code;

    @Column(name = "operation_name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "processing_time_minutes", nullable = false)
    private int processingTimeMinutes;

    protected Operation() {
    }

    private Operation(
            Routing routing,
            Machine machine,
            int sequence,
            String code,
            String name,
            int processingTimeMinutes
    ) {
        this.routing = Objects.requireNonNull(
                routing,
                "routing must not be null"
        );
        this.machine = Objects.requireNonNull(
                machine,
                "machine must not be null"
        );
        if (sequence < 1) {
            throw new IllegalArgumentException(
                    "Operation 순서는 1 이상이어야 합니다."
            );
        }
        this.sequence = sequence;
        this.code = BusinessCodeNormalizer.normalize(
                code,
                "Operation 코드",
                MAX_CODE_LENGTH
        );
        this.name = normalizeName(name);
        if (processingTimeMinutes < 1
                || processingTimeMinutes > MAX_PROCESSING_TIME_MINUTES) {
            throw new IllegalArgumentException(
                    "표준 가공시간은 1분 이상 10080분 이하여야 합니다."
            );
        }
        this.processingTimeMinutes = processingTimeMinutes;
    }

    static Operation create(
            Routing routing,
            Machine machine,
            int sequence,
            String code,
            String name,
            int processingTimeMinutes
    ) {
        return new Operation(
                routing,
                machine,
                sequence,
                code,
                name,
                processingTimeMinutes
        );
    }

    public Long id() {
        return id;
    }

    public Routing routing() {
        return routing;
    }

    public Machine machine() {
        return machine;
    }

    public int sequence() {
        return sequence;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int processingTimeMinutes() {
        return processingTimeMinutes;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Operation 이름은 필수입니다.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Operation 이름은 100자를 초과할 수 없습니다."
            );
        }
        return normalizedName;
    }
}
