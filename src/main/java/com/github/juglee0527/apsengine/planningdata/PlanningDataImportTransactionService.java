package com.github.juglee0527.apsengine.planningdata;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PlanningDataImportTransactionService {

    private static final PlanningDataImportRowError VALIDATION_SKIP_REASON =
            new PlanningDataImportRowError(
                    "row",
                    "FILE_VALIDATION_FAILED",
                    "파일에 검증 오류가 있어 반영하지 않았습니다."
            );
    private static final PlanningDataImportRowError ROLLBACK_SKIP_REASON =
            new PlanningDataImportRowError(
                    "row",
                    "TRANSACTION_ROLLED_BACK",
                    "다른 행의 반영 실패로 전체 트랜잭션을 되돌렸습니다."
            );

    private final PlanningDataImportRunRepository importRunRepository;
    private final PlanningDataImportApplier applier;

    PlanningDataImportTransactionService(
            PlanningDataImportRunRepository importRunRepository,
            PlanningDataImportApplier applier
    ) {
        this.importRunRepository = importRunRepository;
        this.applier = applier;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PlanningDataImportStartResult startOrResume(
            UUID requestKey,
            String fileName,
            String fileSha256,
            int totalRows
    ) {
        PlanningDataImportRun existing = importRunRepository
                .findByRequestKey(requestKey)
                .orElse(null);
        if (existing != null) {
            requireSameFile(existing, fileSha256);
            if (existing.status() == PlanningDataImportStatus.INTERRUPTED) {
                existing.retry(OffsetDateTime.now());
                importRunRepository.saveAndFlush(existing);
                return new PlanningDataImportStartResult(
                        existing.id(),
                        true
                );
            }
            return new PlanningDataImportStartResult(
                    existing.id(),
                    false
            );
        }

        PlanningDataImportRun started = PlanningDataImportRun.start(
                requestKey,
                fileName,
                fileSha256,
                totalRows,
                OffsetDateTime.now()
        );
        importRunRepository.saveAndFlush(started);
        return new PlanningDataImportStartResult(started.id(), true);
    }

    @Transactional(readOnly = true)
    public PlanningDataImportRunResponse findByRequestKey(
            UUID requestKey,
            String fileSha256
    ) {
        PlanningDataImportRun run = importRunRepository
                .findByRequestKey(requestKey)
                .orElse(null);
        if (run == null) {
            return null;
        }
        requireSameFile(run, fileSha256);
        return PlanningDataImportRunResponse.from(run);
    }

    @Transactional(readOnly = true)
    public PlanningDataImportRunResponse find(Long importRunId) {
        return PlanningDataImportRunResponse.from(requireRun(importRunId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void apply(
            Long importRunId,
            List<PlanningDataImportRowPreview> previews
    ) {
        PlanningDataImportRun run = requireRun(importRunId);
        applier.apply(previews);
        List<PlanningDataImportRowResult> rowResults = previews.stream()
                .map(preview -> PlanningDataImportRowResult.succeeded(
                        preview.rowNumber(),
                        parseType(preview.type())
                ))
                .toList();
        run.complete(rowResults, OffsetDateTime.now());
        importRunRepository.saveAndFlush(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failValidation(
            Long importRunId,
            List<PlanningDataImportRowPreview> previews
    ) {
        PlanningDataImportRun run = requireRun(importRunId);
        List<PlanningDataImportRowResult> rowResults = previews.stream()
                .map(preview -> preview.valid()
                        ? PlanningDataImportRowResult.skipped(
                                preview.rowNumber(),
                                parseType(preview.type()),
                                VALIDATION_SKIP_REASON
                        )
                        : PlanningDataImportRowResult.failed(
                                preview.rowNumber(),
                                parseType(preview.type()),
                                preview.errors()
                        ))
                .toList();
        run.fail(
                rowResults,
                "CSV 검증 오류로 데이터를 반영하지 않았습니다.",
                OffsetDateTime.now()
        );
        importRunRepository.saveAndFlush(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failApply(
            Long importRunId,
            List<PlanningDataImportRowPreview> previews,
            PlanningDataApplyException failure
    ) {
        PlanningDataImportRun run = requireRun(importRunId);
        List<PlanningDataImportRowResult> rowResults = new ArrayList<>();
        for (PlanningDataImportRowPreview preview : previews) {
            if (preview.rowNumber() == failure.rowNumber()) {
                rowResults.add(PlanningDataImportRowResult.failed(
                        preview.rowNumber(),
                        failure.dataType(),
                        List.of(new PlanningDataImportRowError(
                                "row",
                                "DB_APPLY_FAILED",
                                failure.getMessage()
                        ))
                ));
            } else {
                rowResults.add(PlanningDataImportRowResult.skipped(
                        preview.rowNumber(),
                        parseType(preview.type()),
                        ROLLBACK_SKIP_REASON
                ));
            }
        }
        run.fail(
                rowResults,
                failure.getMessage(),
                OffsetDateTime.now()
        );
        importRunRepository.saveAndFlush(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int interruptRunning() {
        List<PlanningDataImportRun> running = importRunRepository
                .findAllByStatus(PlanningDataImportStatus.RUNNING);
        OffsetDateTime now = OffsetDateTime.now();
        for (PlanningDataImportRun run : running) {
            run.interrupt(now);
        }
        importRunRepository.saveAllAndFlush(running);
        return running.size();
    }

    private PlanningDataImportRun requireRun(Long importRunId) {
        return importRunRepository.findById(importRunId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.PLANNING_DATA_IMPORT_NOT_FOUND
                ));
    }

    private void requireSameFile(
            PlanningDataImportRun run,
            String fileSha256
    ) {
        if (!run.fileSha256().equals(fileSha256)) {
            throw new ApplicationException(
                    ErrorCode.PLANNING_DATA_IMPORT_REQUEST_CONFLICT
            );
        }
    }

    private PlanningDataType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return PlanningDataType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
