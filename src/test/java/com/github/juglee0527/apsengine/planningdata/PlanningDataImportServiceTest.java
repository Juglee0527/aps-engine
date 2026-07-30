package com.github.juglee0527.apsengine.planningdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PlanningDataImportServiceTest {

    @Mock
    private PlanningDataImportPreviewService previewService;

    @Mock
    private PlanningDataImportTransactionService transactionService;

    private PlanningDataImportService service;

    @BeforeEach
    void setUp() {
        service = new PlanningDataImportService(
                previewService,
                transactionService
        );
    }

    @Test
    void returnsCompletedRunWithoutApplyingSameRequestAgain() {
        UUID requestKey = UUID.randomUUID();
        MockMultipartFile file = file("same-content");
        PlanningDataImportRunResponse completed = run(
                requestKey,
                PlanningDataImportStatus.COMPLETED
        );
        when(transactionService.findByRequestKey(
                eq(requestKey),
                anyString()
        )).thenReturn(completed);

        PlanningDataImportRunResponse response =
                service.execute(requestKey, file);

        assertThat(response).isSameAs(completed);
        verify(previewService, never()).preview(
                org.mockito.ArgumentMatchers.any(byte[].class)
        );
        verify(transactionService, never()).apply(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void appliesEveryValidPreviewRow() {
        UUID requestKey = UUID.randomUUID();
        MockMultipartFile file = file("valid-content");
        PlanningDataImportRowPreview row = validFactoryRow();
        PlanningDataImportPreviewResponse preview =
                PlanningDataImportPreviewResponse.from(List.of(row));
        PlanningDataImportRunResponse completed = run(
                requestKey,
                PlanningDataImportStatus.COMPLETED
        );
        when(transactionService.findByRequestKey(
                eq(requestKey),
                anyString()
        )).thenReturn(null);
        when(previewService.preview(
                org.mockito.ArgumentMatchers.any(byte[].class)
        )).thenReturn(preview);
        when(transactionService.startOrResume(
                eq(requestKey),
                eq("planning-data.csv"),
                anyString(),
                eq(1)
        )).thenReturn(new PlanningDataImportStartResult(7L, true));
        when(transactionService.find(7L)).thenReturn(completed);

        PlanningDataImportRunResponse response =
                service.execute(requestKey, file);

        assertThat(response.status())
                .isEqualTo(PlanningDataImportStatus.COMPLETED);
        verify(transactionService).apply(7L, preview.rows());
        verify(transactionService, never()).failValidation(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void recordsValidationFailureWithoutCallingApplier() {
        UUID requestKey = UUID.randomUUID();
        MockMultipartFile file = file("invalid-content");
        PlanningDataImportRowPreview row =
                new PlanningDataImportRowPreview(
                        2,
                        "PRODUCTION_LINE",
                        false,
                        Map.of(),
                        List.of(new PlanningDataImportRowError(
                                "factoryCode",
                                "REFERENCE_NOT_FOUND",
                                "공장을 찾을 수 없습니다."
                        ))
                );
        PlanningDataImportPreviewResponse preview =
                PlanningDataImportPreviewResponse.from(List.of(row));
        PlanningDataImportRunResponse failed = run(
                requestKey,
                PlanningDataImportStatus.FAILED
        );
        when(transactionService.findByRequestKey(
                eq(requestKey),
                anyString()
        )).thenReturn(null);
        when(previewService.preview(
                org.mockito.ArgumentMatchers.any(byte[].class)
        )).thenReturn(preview);
        when(transactionService.startOrResume(
                eq(requestKey),
                eq("planning-data.csv"),
                anyString(),
                eq(1)
        )).thenReturn(new PlanningDataImportStartResult(8L, true));
        when(transactionService.find(8L)).thenReturn(failed);

        PlanningDataImportRunResponse response =
                service.execute(requestKey, file);

        assertThat(response.status())
                .isEqualTo(PlanningDataImportStatus.FAILED);
        verify(transactionService).failValidation(8L, preview.rows());
        verify(transactionService, never()).apply(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void recordsApplyFailureAfterTransactionRollback() {
        UUID requestKey = UUID.randomUUID();
        MockMultipartFile file = file("valid-content");
        PlanningDataImportRowPreview row = validFactoryRow();
        PlanningDataImportPreviewResponse preview =
                PlanningDataImportPreviewResponse.from(List.of(row));
        PlanningDataApplyException failure =
                new PlanningDataApplyException(
                        2,
                        PlanningDataType.FACTORY,
                        "CSV 2행 반영에 실패했습니다.",
                        new RuntimeException("duplicate")
                );
        PlanningDataImportRunResponse failed = run(
                requestKey,
                PlanningDataImportStatus.FAILED
        );
        when(transactionService.findByRequestKey(
                eq(requestKey),
                anyString()
        )).thenReturn(null);
        when(previewService.preview(
                org.mockito.ArgumentMatchers.any(byte[].class)
        )).thenReturn(preview);
        when(transactionService.startOrResume(
                eq(requestKey),
                eq("planning-data.csv"),
                anyString(),
                eq(1)
        )).thenReturn(new PlanningDataImportStartResult(9L, true));
        doThrow(failure).when(transactionService)
                .apply(9L, preview.rows());
        when(transactionService.find(9L)).thenReturn(failed);

        PlanningDataImportRunResponse response =
                service.execute(requestKey, file);

        assertThat(response.status())
                .isEqualTo(PlanningDataImportStatus.FAILED);
        verify(transactionService)
                .failApply(9L, preview.rows(), failure);
    }

    @Test
    void rejectsDifferentFileForExistingRequestKey() {
        UUID requestKey = UUID.randomUUID();
        ApplicationException conflict = new ApplicationException(
                ErrorCode.PLANNING_DATA_IMPORT_REQUEST_CONFLICT
        );
        when(transactionService.findByRequestKey(
                eq(requestKey),
                anyString()
        )).thenThrow(conflict);

        assertThatThrownBy(() ->
                service.execute(requestKey, file("different-content")))
                .isSameAs(conflict);
    }

    private MockMultipartFile file(String contents) {
        return new MockMultipartFile(
                "file",
                "planning-data.csv",
                "text/csv",
                contents.getBytes(StandardCharsets.UTF_8)
        );
    }

    private PlanningDataImportRowPreview validFactoryRow() {
        return new PlanningDataImportRowPreview(
                2,
                "FACTORY",
                true,
                Map.of(
                        "factoryCode", "FACTORY-01",
                        "name", "공장"
                ),
                List.of()
        );
    }

    private PlanningDataImportRunResponse run(
            UUID requestKey,
            PlanningDataImportStatus status
    ) {
        OffsetDateTime now =
                OffsetDateTime.parse("2026-07-30T10:00:00+09:00");
        int successRows =
                status == PlanningDataImportStatus.COMPLETED ? 1 : 0;
        int failedRows =
                status == PlanningDataImportStatus.FAILED ? 1 : 0;
        return new PlanningDataImportRunResponse(
                7L,
                requestKey,
                "planning-data.csv",
                "a".repeat(64),
                status,
                1,
                successRows,
                failedRows,
                0,
                0,
                failedRows == 0 ? null : "실패",
                now,
                now,
                now,
                List.of()
        );
    }
}
