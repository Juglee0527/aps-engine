package com.github.juglee0527.apsengine.planningdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("local")
@SpringBootTest
@Transactional
@Rollback
@EnabledIfEnvironmentVariable(
        named = "APS_POSTGRES_INTEGRATION_TEST",
        matches = "true"
)
class PlanningDataImportJpaIntegrationTest {

    @Autowired
    private PlanningDataImportPreviewService previewService;

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
    private EntityManager entityManager;

    @Test
    void previewLeavesEveryPlanningTableUnchanged() {
        long[] before = counts();
        String suffix = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
        String factoryCode = "F-" + suffix;
        String lineCode = "L-" + suffix;
        String machineCode = "M-" + suffix;
        String productCode = "P-" + suffix;
        String routingCode = "R-" + suffix;
        String csv = csv(
                row(Map.of(
                        "type", "FACTORY",
                        "factoryCode", factoryCode,
                        "name", "미리보기 공장"
                )),
                row(Map.of(
                        "type", "PRODUCTION_LINE",
                        "factoryCode", factoryCode,
                        "lineCode", lineCode,
                        "name", "미리보기 라인"
                )),
                row(Map.of(
                        "type", "MACHINE",
                        "factoryCode", factoryCode,
                        "lineCode", lineCode,
                        "machineCode", machineCode,
                        "name", "미리보기 설비",
                        "status", "AVAILABLE"
                )),
                row(Map.of(
                        "type", "PRODUCT",
                        "productCode", productCode,
                        "name", "미리보기 품목",
                        "unit", "PIECE"
                )),
                row(Map.ofEntries(
                        Map.entry("type", "ROUTING"),
                        Map.entry("factoryCode", factoryCode),
                        Map.entry("lineCode", lineCode),
                        Map.entry("machineCode", machineCode),
                        Map.entry("productCode", productCode),
                        Map.entry("routingCode", routingCode),
                        Map.entry("name", "미리보기 Routing"),
                        Map.entry("operationSequence", "10"),
                        Map.entry("operationCode", "PROCESS"),
                        Map.entry("operationName", "가공"),
                        Map.entry("processingTimeMinutes", "30")
                )),
                row(Map.ofEntries(
                        Map.entry("type", "PRODUCTION_ORDER"),
                        Map.entry("productCode", productCode),
                        Map.entry("routingCode", routingCode),
                        Map.entry("orderNumber", "PO-" + suffix),
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

        PlanningDataImportPreviewResponse response = previewService.preview(
                new MockMultipartFile(
                        "file",
                        "planning-data.csv",
                        "text/csv",
                        csv.getBytes(StandardCharsets.UTF_8)
                )
        );
        entityManager.flush();

        assertThat(response.readyToApply()).isTrue();
        assertThat(counts()).containsExactly(before);
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

    private String csv(String... rows) {
        return PlanningDataCsvTestSupport.header()
                + "\n"
                + String.join("\n", rows);
    }

    private String row(Map<String, String> fields) {
        return PlanningDataCsvTestSupport.row(fields);
    }
}
