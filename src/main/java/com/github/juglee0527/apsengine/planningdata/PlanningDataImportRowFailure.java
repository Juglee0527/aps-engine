package com.github.juglee0527.apsengine.planningdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "planning_data_import_row_error")
public class PlanningDataImportRowFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planning_data_import_row_error_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "planning_data_import_row_id",
            nullable = false,
            updatable = false
    )
    private PlanningDataImportRowResult rowResult;

    @Column(name = "error_field", nullable = false, length = 50)
    private String field;

    @Column(name = "error_code", nullable = false, length = 50)
    private String code;

    @Column(name = "error_message", nullable = false, length = 500)
    private String message;

    protected PlanningDataImportRowFailure() {
    }

    private PlanningDataImportRowFailure(
            PlanningDataImportRowResult rowResult,
            PlanningDataImportRowError error
    ) {
        this.rowResult = rowResult;
        this.field = limit(error.field(), 50);
        this.code = limit(error.code(), 50);
        this.message = limit(error.message(), 500);
    }

    static PlanningDataImportRowFailure create(
            PlanningDataImportRowResult rowResult,
            PlanningDataImportRowError error
    ) {
        return new PlanningDataImportRowFailure(rowResult, error);
    }

    public String field() {
        return field;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "행 오류 값은 필수입니다."
            );
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
