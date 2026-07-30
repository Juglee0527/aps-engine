package com.github.juglee0527.apsengine.planningdata;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PlanningDataImportService {

    private final PlanningDataImportPreviewService previewService;
    private final PlanningDataImportTransactionService transactionService;

    public PlanningDataImportService(
            PlanningDataImportPreviewService previewService,
            PlanningDataImportTransactionService transactionService
    ) {
        this.previewService = previewService;
        this.transactionService = transactionService;
    }

    public PlanningDataImportRunResponse execute(
            UUID requestKey,
            MultipartFile file
    ) {
        if (requestKey == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "requestKey는 필수입니다."
            );
        }
        byte[] fileBytes =
                PlanningDataImportPreviewService.readValidatedBytes(file);
        String fileSha256 = sha256(fileBytes);
        PlanningDataImportRunResponse existing =
                transactionService.findByRequestKey(
                        requestKey,
                        fileSha256
                );
        if (existing != null
                && existing.status()
                != PlanningDataImportStatus.INTERRUPTED) {
            return existing;
        }

        PlanningDataImportPreviewResponse preview =
                previewService.preview(fileBytes);
        PlanningDataImportStartResult startResult;
        try {
            startResult = transactionService.startOrResume(
                    requestKey,
                    fileName(file),
                    fileSha256,
                    preview.totalRows()
            );
        } catch (DataIntegrityViolationException exception) {
            PlanningDataImportRunResponse concurrent =
                    transactionService.findByRequestKey(
                            requestKey,
                            fileSha256
                    );
            if (concurrent == null) {
                throw exception;
            }
            return concurrent;
        }
        if (!startResult.shouldApply()) {
            return transactionService.find(startResult.importRunId());
        }
        if (!preview.readyToApply()) {
            transactionService.failValidation(
                    startResult.importRunId(),
                    preview.rows()
            );
            return transactionService.find(startResult.importRunId());
        }

        try {
            transactionService.apply(
                    startResult.importRunId(),
                    preview.rows()
            );
        } catch (PlanningDataApplyException exception) {
            transactionService.failApply(
                    startResult.importRunId(),
                    preview.rows(),
                    exception
            );
        } catch (RuntimeException exception) {
            PlanningDataImportRowPreview firstRow =
                    preview.rows().getFirst();
            PlanningDataApplyException failure =
                    new PlanningDataApplyException(
                            firstRow.rowNumber(),
                            PlanningDataType.valueOf(firstRow.type()),
                            "CSV 반영 트랜잭션에 실패했습니다: "
                                    + rootMessage(exception),
                            exception
                    );
            transactionService.failApply(
                    startResult.importRunId(),
                    preview.rows(),
                    failure
            );
        }
        return transactionService.find(startResult.importRunId());
    }

    public PlanningDataImportRunResponse find(Long importRunId) {
        return transactionService.find(importRunId);
    }

    private String fileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.trim().length() > 255) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "파일 이름은 255자를 초과할 수 없습니다."
            );
        }
        return fileName;
    }

    private String sha256(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(fileBytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 해시 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null
                ? throwable.getClass().getSimpleName()
                : cause.getMessage();
    }
}
