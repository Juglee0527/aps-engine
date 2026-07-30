package com.github.juglee0527.apsengine.planningdata;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "planning_data_import_run",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_planning_data_import_request_key",
                columnNames = "request_key"
        )
)
public class PlanningDataImportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planning_data_import_run_id")
    private Long id;

    @Column(name = "request_key", nullable = false, updatable = false)
    private UUID requestKey;

    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    private String fileName;

    @Column(name = "file_sha256", nullable = false, updatable = false, length = 64)
    private String fileSha256;

    @Column(name = "total_rows", nullable = false, updatable = false)
    private int totalRows;

    @Column(name = "success_rows", nullable = false)
    private int successRows;

    @Column(name = "failed_rows", nullable = false)
    private int failedRows;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanningDataImportStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @OneToMany(
            mappedBy = "importRun",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("rowNumber ASC, id ASC")
    private Set<PlanningDataImportRowResult> rows =
            new LinkedHashSet<>();

    protected PlanningDataImportRun() {
    }

    private PlanningDataImportRun(
            UUID requestKey,
            String fileName,
            String fileSha256,
            int totalRows,
            OffsetDateTime now
    ) {
        this.requestKey = Objects.requireNonNull(
                requestKey,
                "requestKey must not be null"
        );
        this.fileName = normalizeFileName(fileName);
        this.fileSha256 = validateSha256(fileSha256);
        if (totalRows < 1
                || totalRows
                > PlanningDataImportPreviewService.MAX_DATA_ROWS) {
            throw new IllegalArgumentException(
                    "입력 행 수가 허용 범위를 벗어났습니다."
            );
        }
        this.totalRows = totalRows;
        this.status = PlanningDataImportStatus.RUNNING;
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.startedAt = now;
    }

    public static PlanningDataImportRun start(
            UUID requestKey,
            String fileName,
            String fileSha256,
            int totalRows,
            OffsetDateTime now
    ) {
        return new PlanningDataImportRun(
                requestKey,
                fileName,
                fileSha256,
                totalRows,
                now
        );
    }

    public void complete(
            List<PlanningDataImportRowResult> rowResults,
            OffsetDateTime now
    ) {
        requireStatus(PlanningDataImportStatus.RUNNING);
        replaceRows(rowResults);
        this.successRows = totalRows;
        this.failedRows = 0;
        this.failureReason = null;
        this.status = PlanningDataImportStatus.COMPLETED;
        this.completedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void fail(
            List<PlanningDataImportRowResult> rowResults,
            String failureReason,
            OffsetDateTime now
    ) {
        requireStatus(PlanningDataImportStatus.RUNNING);
        replaceRows(rowResults);
        this.successRows = 0;
        this.failedRows = (int) rowResults.stream()
                .filter(row ->
                        row.status() == PlanningDataImportRowStatus.FAILED)
                .count();
        this.failureReason = normalizeFailureReason(failureReason);
        this.status = PlanningDataImportStatus.FAILED;
        this.completedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void interrupt(OffsetDateTime now) {
        requireStatus(PlanningDataImportStatus.RUNNING);
        this.status = PlanningDataImportStatus.INTERRUPTED;
        this.failureReason = "애플리케이션 재시작으로 실행이 중단되었습니다.";
        this.completedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void retry(OffsetDateTime now) {
        requireStatus(PlanningDataImportStatus.INTERRUPTED);
        rows.clear();
        successRows = 0;
        failedRows = 0;
        failureReason = null;
        status = PlanningDataImportStatus.RUNNING;
        startedAt = Objects.requireNonNull(now, "now must not be null");
        completedAt = null;
        retryCount++;
    }

    private void replaceRows(
            List<PlanningDataImportRowResult> rowResults
    ) {
        if (rowResults == null || rowResults.size() != totalRows) {
            throw new IllegalArgumentException(
                    "행 결과 수는 전체 입력 행 수와 같아야 합니다."
            );
        }
        rows.clear();
        for (PlanningDataImportRowResult row : rowResults) {
            row.attach(this);
            rows.add(row);
        }
    }

    public Long id() {
        return id;
    }

    public UUID requestKey() {
        return requestKey;
    }

    public String fileName() {
        return fileName;
    }

    public String fileSha256() {
        return fileSha256;
    }

    public int totalRows() {
        return totalRows;
    }

    public int successRows() {
        return successRows;
    }

    public int failedRows() {
        return failedRows;
    }

    public int retryCount() {
        return retryCount;
    }

    public PlanningDataImportStatus status() {
        return status;
    }

    public String failureReason() {
        return failureReason;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime startedAt() {
        return startedAt;
    }

    public OffsetDateTime completedAt() {
        return completedAt;
    }

    public List<PlanningDataImportRowResult> rows() {
        return List.copyOf(rows);
    }

    private void requireStatus(PlanningDataImportStatus required) {
        if (status != required) {
            throw new IllegalStateException(
                    "입력 실행 상태를 %s에서 변경할 수 없습니다."
                            .formatted(status)
            );
        }
    }

    private static String normalizeFileName(String fileName) {
        String normalized = fileName == null || fileName.isBlank()
                ? "planning-data.csv"
                : fileName.trim();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException(
                    "파일 이름은 255자를 초과할 수 없습니다."
            );
        }
        return normalized;
    }

    private static String validateSha256(String fileSha256) {
        if (fileSha256 == null
                || !fileSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "파일 SHA-256 형식이 올바르지 않습니다."
            );
        }
        return fileSha256;
    }

    private static String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "계획 데이터 입력에 실패했습니다.";
        }
        String normalized = failureReason.trim();
        return normalized.length() <= 500
                ? normalized
                : normalized.substring(0, 500);
    }
}
