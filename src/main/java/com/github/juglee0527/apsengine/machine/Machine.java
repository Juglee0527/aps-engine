package com.github.juglee0527.apsengine.machine;

import java.util.Objects;

import com.github.juglee0527.apsengine.common.domain.BusinessCodeNormalizer;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "machine",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_machine_line_code",
                columnNames = {"production_line_id", "machine_code"}
        )
)
public class Machine {

    static final int MAX_CODE_LENGTH = 50;
    static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "machine_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "production_line_id",
            nullable = false,
            updatable = false
    )
    private ProductionLine productionLine;

    @Column(
            name = "machine_code",
            nullable = false,
            updatable = false,
            length = MAX_CODE_LENGTH
    )
    private String code;

    @Column(name = "machine_name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MachineStatus status;

    protected Machine() {
    }

    private Machine(ProductionLine productionLine, String code, String name) {
        this(productionLine, code, name, MachineStatus.AVAILABLE);
    }

    private Machine(
            ProductionLine productionLine,
            String code,
            String name,
            MachineStatus status
    ) {
        this.productionLine = validateProductionLine(productionLine);
        this.code = BusinessCodeNormalizer.normalize(
                code,
                "설비 코드",
                MAX_CODE_LENGTH
        );
        this.name = normalizeName(name);
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static Machine create(
            ProductionLine productionLine,
            String code,
            String name
    ) {
        return new Machine(productionLine, code, name);
    }

    public static Machine create(
            ProductionLine productionLine,
            String code,
            String name,
            MachineStatus status
    ) {
        return new Machine(productionLine, code, name, status);
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public void stop() {
        changeStatus(MachineStatus.AVAILABLE, MachineStatus.STOPPED);
    }

    public void restart() {
        changeStatus(MachineStatus.STOPPED, MachineStatus.AVAILABLE);
    }

    public void deactivate() {
        if (status == MachineStatus.INACTIVE) {
            throw invalidStatusTransition(MachineStatus.INACTIVE);
        }
        status = MachineStatus.INACTIVE;
    }

    public void reactivate() {
        changeStatus(MachineStatus.INACTIVE, MachineStatus.AVAILABLE);
    }

    public Long id() {
        return id;
    }

    public ProductionLine productionLine() {
        return productionLine;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public MachineStatus status() {
        return status;
    }

    private void changeStatus(
            MachineStatus requiredCurrentStatus,
            MachineStatus nextStatus
    ) {
        if (status != requiredCurrentStatus) {
            throw invalidStatusTransition(nextStatus);
        }
        status = nextStatus;
    }

    private IllegalStateException invalidStatusTransition(
            MachineStatus nextStatus
    ) {
        return new IllegalStateException(
                "설비 상태를 %s에서 %s(으)로 변경할 수 없습니다."
                        .formatted(status, nextStatus)
        );
    }

    private static ProductionLine validateProductionLine(
            ProductionLine productionLine
    ) {
        Objects.requireNonNull(
                productionLine,
                "productionLine must not be null"
        );
        if (!productionLine.isActive()) {
            throw new IllegalStateException(
                    "비활성 생산라인에는 설비를 등록할 수 없습니다."
            );
        }
        return productionLine;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("설비 이름은 필수입니다.");
        }

        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "설비 이름은 100자를 초과할 수 없습니다."
            );
        }
        return normalizedName;
    }
}
