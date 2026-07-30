package com.github.juglee0527.apsengine.planningdata;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        name = "planning_data_import_row",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_planning_data_import_row_number",
                columnNames = {
                        "planning_data_import_run_id",
                        "row_number"
                }
        )
)
public class PlanningDataImportRowResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planning_data_import_row_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "planning_data_import_run_id",
            nullable = false,
            updatable = false
    )
    private PlanningDataImportRun importRun;

    @Column(name = "row_number", nullable = false, updatable = false)
    private int rowNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 30, updatable = false)
    private PlanningDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanningDataImportRowStatus status;

    @OneToMany(
            mappedBy = "rowResult",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private Set<PlanningDataImportRowFailure> errors =
            new LinkedHashSet<>();

    protected PlanningDataImportRowResult() {
    }

    private PlanningDataImportRowResult(
            int rowNumber,
            PlanningDataType dataType,
            PlanningDataImportRowStatus status,
            List<PlanningDataImportRowError> errors
    ) {
        if (rowNumber < 2) {
            throw new IllegalArgumentException(
                    "CSV 데이터 행 번호는 2 이상이어야 합니다."
            );
        }
        this.rowNumber = rowNumber;
        this.dataType = dataType;
        this.status = status;
        for (PlanningDataImportRowError error : errors) {
            this.errors.add(PlanningDataImportRowFailure.create(
                    this,
                    error
            ));
        }
    }

    public static PlanningDataImportRowResult succeeded(
            int rowNumber,
            PlanningDataType dataType
    ) {
        return new PlanningDataImportRowResult(
                rowNumber,
                dataType,
                PlanningDataImportRowStatus.SUCCEEDED,
                List.of()
        );
    }

    public static PlanningDataImportRowResult failed(
            int rowNumber,
            PlanningDataType dataType,
            List<PlanningDataImportRowError> errors
    ) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "실패 행에는 오류가 하나 이상 필요합니다."
            );
        }
        return new PlanningDataImportRowResult(
                rowNumber,
                dataType,
                PlanningDataImportRowStatus.FAILED,
                errors
        );
    }

    public static PlanningDataImportRowResult skipped(
            int rowNumber,
            PlanningDataType dataType,
            PlanningDataImportRowError reason
    ) {
        return new PlanningDataImportRowResult(
                rowNumber,
                dataType,
                PlanningDataImportRowStatus.SKIPPED,
                List.of(reason)
        );
    }

    void attach(PlanningDataImportRun importRun) {
        if (this.importRun != null) {
            throw new IllegalStateException(
                    "행 결과는 하나의 입력 실행에만 속할 수 있습니다."
            );
        }
        this.importRun = importRun;
    }

    public int rowNumber() {
        return rowNumber;
    }

    public PlanningDataType dataType() {
        return dataType;
    }

    public PlanningDataImportRowStatus status() {
        return status;
    }

    public List<PlanningDataImportRowFailure> errors() {
        return List.copyOf(errors);
    }
}
