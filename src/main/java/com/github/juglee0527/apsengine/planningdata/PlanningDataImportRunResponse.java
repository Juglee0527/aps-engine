package com.github.juglee0527.apsengine.planningdata;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PlanningDataImportRunResponse(
        Long id,
        UUID requestKey,
        String fileName,
        String fileSha256,
        PlanningDataImportStatus status,
        int totalRows,
        int successRows,
        int failedRows,
        int skippedRows,
        int retryCount,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        List<PlanningDataImportRowResultResponse> rows
) {

    public static PlanningDataImportRunResponse from(
            PlanningDataImportRun run
    ) {
        List<PlanningDataImportRowResultResponse> rows = run.rows().stream()
                .map(PlanningDataImportRowResultResponse::from)
                .toList();
        int skippedRows = (int) rows.stream()
                .filter(row ->
                        row.status() == PlanningDataImportRowStatus.SKIPPED)
                .count();
        return new PlanningDataImportRunResponse(
                run.id(),
                run.requestKey(),
                run.fileName(),
                run.fileSha256(),
                run.status(),
                run.totalRows(),
                run.successRows(),
                run.failedRows(),
                skippedRows,
                run.retryCount(),
                run.failureReason(),
                run.createdAt(),
                run.startedAt(),
                run.completedAt(),
                rows
        );
    }
}
