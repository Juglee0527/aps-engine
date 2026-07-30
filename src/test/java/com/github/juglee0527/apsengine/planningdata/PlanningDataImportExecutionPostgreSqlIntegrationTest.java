package com.github.juglee0527.apsengine.planningdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;
import com.github.juglee0527.apsengine.support.PostgreSqlContainerIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

class PlanningDataImportExecutionPostgreSqlIntegrationTest
        extends PostgreSqlContainerIntegrationTest {

    @Autowired
    private PlanningDataImportService importService;

    @Autowired
    private PlanningDataImportTransactionService transactionService;

    @Autowired
    private FactoryRepository factoryRepository;

    @Autowired
    private ProductionLineRepository lineRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RoutingRepository routingRepository;

    @Autowired
    private ProductionOrderRepository orderRepository;

    @Autowired
    private PlanningDataImportRunRepository importRunRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearPlanningData() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE planning_data_import_run, "
                        + "factory, product RESTART IDENTITY CASCADE"
        );
    }

    @Test
    void appliesCompleteFileAndReturnsSameResultForDuplicateRequest() {
        UUID requestKey = UUID.randomUUID();
        MockMultipartFile file = file(completeCsv("IDEMPOTENT"));

        PlanningDataImportRunResponse first =
                importService.execute(requestKey, file);
        PlanningDataImportRunResponse second =
                importService.execute(requestKey, file);

        assertThat(first.status())
                .isEqualTo(PlanningDataImportStatus.COMPLETED);
        assertThat(first.successRows()).isEqualTo(6);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(importRunRepository.count()).isEqualTo(1);
        assertThat(counts()).containsExactly(1, 1, 1, 1, 1, 1);
    }

    @Test
    void rejectsDifferentFileForSameRequestKey() {
        UUID requestKey = UUID.randomUUID();
        importService.execute(
                requestKey,
                file(singleFactoryCsv("FIRST"))
        );

        assertThatThrownBy(() -> importService.execute(
                requestKey,
                file(singleFactoryCsv("SECOND"))
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode())
                        .isEqualTo(
                                ErrorCode
                                        .PLANNING_DATA_IMPORT_REQUEST_CONFLICT
                        )
        );
        assertThat(factoryRepository.count()).isEqualTo(1);
        assertThat(importRunRepository.count()).isEqualTo(1);
    }

    @Test
    void recordsReferenceFailureAndLeavesPlanningTablesEmpty() {
        String csv = csv(
                row(Map.of(
                        "type", "FACTORY",
                        "factoryCode", "VALID-FACTORY",
                        "name", "유효하지만 미반영 공장"
                )),
                row(Map.ofEntries(
                        Map.entry("type", "ROUTING"),
                        Map.entry("factoryCode", "MISSING-FACTORY"),
                        Map.entry("lineCode", "MISSING-LINE"),
                        Map.entry("machineCode", "MISSING-MACHINE"),
                        Map.entry("productCode", "MISSING-PRODUCT"),
                        Map.entry("routingCode", "ROUTING-01"),
                        Map.entry("name", "잘못된 참조 Routing"),
                        Map.entry("operationSequence", "10"),
                        Map.entry("operationCode", "PROCESS"),
                        Map.entry("operationName", "가공"),
                        Map.entry("processingTimeMinutes", "30")
                ))
        );

        PlanningDataImportRunResponse response = importService.execute(
                UUID.randomUUID(),
                file(csv)
        );

        assertThat(response.status())
                .isEqualTo(PlanningDataImportStatus.FAILED);
        assertThat(response.successRows()).isZero();
        assertThat(response.failedRows()).isEqualTo(1);
        assertThat(response.skippedRows()).isEqualTo(1);
        assertThat(response.rows())
                .extracting(PlanningDataImportRowResultResponse::status)
                .containsExactly(
                        PlanningDataImportRowStatus.SKIPPED,
                        PlanningDataImportRowStatus.FAILED
                );
        assertThat(response.rows().get(1).errors()).hasSize(2);
        assertThat(counts()).containsOnly(0L);
    }

    @Test
    void recordsDatabaseConstraintFailureAfterAtomicRollback() {
        factoryRepository.saveAndFlush(
                Factory.create("DUPLICATE", "기존 공장")
        );
        UUID requestKey = UUID.randomUUID();
        PlanningDataImportStartResult started =
                transactionService.startOrResume(
                        requestKey,
                        "duplicate.csv",
                        "b".repeat(64),
                        1
                );
        PlanningDataImportRowPreview duplicate =
                new PlanningDataImportRowPreview(
                        2,
                        "FACTORY",
                        true,
                        Map.of(
                                "factoryCode", "DUPLICATE",
                                "name", "중복 공장"
                        ),
                        List.of()
                );

        PlanningDataApplyException failure = catchThrowableOfType(
                () -> transactionService.apply(
                        started.importRunId(),
                        List.of(duplicate)
                ),
                PlanningDataApplyException.class
        );
        assertThat(failure).isNotNull();
        transactionService.failApply(
                started.importRunId(),
                List.of(duplicate),
                failure
        );

        PlanningDataImportRunResponse response =
                transactionService.find(started.importRunId());
        assertThat(response.status())
                .isEqualTo(PlanningDataImportStatus.FAILED);
        assertThat(response.rows().getFirst().errors())
                .extracting(PlanningDataImportRowError::code)
                .containsExactly("DB_APPLY_FAILED");
        assertThat(factoryRepository.count()).isEqualTo(1);
    }

    @Test
    void retriesInterruptedRunWithSameFile() throws Exception {
        UUID requestKey = UUID.randomUUID();
        MockMultipartFile file = file(singleFactoryCsv("RETRY"));
        PlanningDataImportStartResult started =
                transactionService.startOrResume(
                        requestKey,
                        file.getOriginalFilename(),
                        sha256(file.getBytes()),
                        1
                );
        assertThat(transactionService.interruptRunning()).isEqualTo(1);

        PlanningDataImportRunResponse response =
                importService.execute(requestKey, file);

        assertThat(response.id()).isEqualTo(started.importRunId());
        assertThat(response.status())
                .isEqualTo(PlanningDataImportStatus.COMPLETED);
        assertThat(response.retryCount()).isEqualTo(1);
        assertThat(factoryRepository.count()).isEqualTo(1);
    }

    @Test
    @Timeout(180)
    void appliesMaximumTwoThousandRows() {
        List<String> rows = new ArrayList<>();
        for (int index = 1;
                index <= PlanningDataImportPreviewService.MAX_DATA_ROWS;
                index++) {
            rows.add(row(Map.of(
                    "type", "FACTORY",
                    "factoryCode", "BULK-%04d".formatted(index),
                    "name", "대량 공장 %04d".formatted(index)
            )));
        }

        PlanningDataImportRunResponse response = importService.execute(
                UUID.randomUUID(),
                file(csv(rows.toArray(String[]::new)))
        );

        assertThat(response.status())
                .isEqualTo(PlanningDataImportStatus.COMPLETED);
        assertThat(response.totalRows())
                .isEqualTo(PlanningDataImportPreviewService.MAX_DATA_ROWS);
        assertThat(response.successRows())
                .isEqualTo(PlanningDataImportPreviewService.MAX_DATA_ROWS);
        assertThat(factoryRepository.count())
                .isEqualTo(PlanningDataImportPreviewService.MAX_DATA_ROWS);
    }

    private long[] counts() {
        return new long[]{
                factoryRepository.count(),
                lineRepository.count(),
                machineRepository.count(),
                productRepository.count(),
                routingRepository.count(),
                orderRepository.count()
        };
    }

    private String completeCsv(String suffix) {
        String factoryCode = "FACTORY-" + suffix;
        String lineCode = "LINE-" + suffix;
        String machineCode = "MACHINE-" + suffix;
        String productCode = "PRODUCT-" + suffix;
        String routingCode = "ROUTING-" + suffix;
        return csv(
                row(Map.of(
                        "type", "FACTORY",
                        "factoryCode", factoryCode,
                        "name", "공장"
                )),
                row(Map.of(
                        "type", "PRODUCTION_LINE",
                        "factoryCode", factoryCode,
                        "lineCode", lineCode,
                        "name", "라인"
                )),
                row(Map.of(
                        "type", "MACHINE",
                        "factoryCode", factoryCode,
                        "lineCode", lineCode,
                        "machineCode", machineCode,
                        "name", "설비",
                        "status", "AVAILABLE"
                )),
                row(Map.of(
                        "type", "PRODUCT",
                        "productCode", productCode,
                        "name", "품목",
                        "unit", "PIECE"
                )),
                row(Map.ofEntries(
                        Map.entry("type", "ROUTING"),
                        Map.entry("factoryCode", factoryCode),
                        Map.entry("lineCode", lineCode),
                        Map.entry("machineCode", machineCode),
                        Map.entry("productCode", productCode),
                        Map.entry("routingCode", routingCode),
                        Map.entry("name", "Routing"),
                        Map.entry("operationSequence", "10"),
                        Map.entry("operationCode", "PROCESS"),
                        Map.entry("operationName", "가공"),
                        Map.entry("processingTimeMinutes", "30")
                )),
                row(Map.ofEntries(
                        Map.entry("type", "PRODUCTION_ORDER"),
                        Map.entry("productCode", productCode),
                        Map.entry("routingCode", routingCode),
                        Map.entry("orderNumber", "ORDER-" + suffix),
                        Map.entry("quantity", "10"),
                        Map.entry(
                                "releaseAt",
                                "2026-08-03T08:00:00+09:00"
                        ),
                        Map.entry(
                                "dueAt",
                                "2026-08-04T18:00:00+09:00"
                        ),
                        Map.entry("priority", "80")
                ))
        );
    }

    private String singleFactoryCsv(String suffix) {
        return csv(row(Map.of(
                "type", "FACTORY",
                "factoryCode", "FACTORY-" + suffix,
                "name", "공장 " + suffix
        )));
    }

    private MockMultipartFile file(String csv) {
        return new MockMultipartFile(
                "file",
                "planning-data.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String csv(String... rows) {
        return PlanningDataCsvTestSupport.header()
                + "\n"
                + String.join("\n", rows);
    }

    private String row(Map<String, String> fields) {
        return PlanningDataCsvTestSupport.row(fields);
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
