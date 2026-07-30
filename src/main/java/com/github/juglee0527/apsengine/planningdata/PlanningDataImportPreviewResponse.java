package com.github.juglee0527.apsengine.planningdata;

import java.util.List;

public record PlanningDataImportPreviewResponse(
        boolean readyToApply,
        int totalRows,
        int validRows,
        int invalidRows,
        List<PlanningDataImportRowPreview> rows
) {

    public PlanningDataImportPreviewResponse {
        rows = List.copyOf(rows);
    }

    static PlanningDataImportPreviewResponse from(
            List<PlanningDataImportRowPreview> rows
    ) {
        int validRows = (int) rows.stream()
                .filter(PlanningDataImportRowPreview::valid)
                .count();
        int invalidRows = rows.size() - validRows;
        return new PlanningDataImportPreviewResponse(
                invalidRows == 0,
                rows.size(),
                validRows,
                invalidRows,
                rows
        );
    }
}
