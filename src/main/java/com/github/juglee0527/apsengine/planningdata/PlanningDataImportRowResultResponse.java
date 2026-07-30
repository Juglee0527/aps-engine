package com.github.juglee0527.apsengine.planningdata;

import java.util.List;

public record PlanningDataImportRowResultResponse(
        int rowNumber,
        PlanningDataType type,
        PlanningDataImportRowStatus status,
        List<PlanningDataImportRowError> errors
) {

    static PlanningDataImportRowResultResponse from(
            PlanningDataImportRowResult row
    ) {
        return new PlanningDataImportRowResultResponse(
                row.rowNumber(),
                row.dataType(),
                row.status(),
                row.errors().stream()
                        .map(error -> new PlanningDataImportRowError(
                                error.field(),
                                error.code(),
                                error.message()
                        ))
                        .toList()
        );
    }
}
