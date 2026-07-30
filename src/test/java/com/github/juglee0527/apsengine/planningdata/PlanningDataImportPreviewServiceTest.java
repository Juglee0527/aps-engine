package com.github.juglee0527.apsengine.planningdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class PlanningDataImportPreviewServiceTest {

    @Mock
    private FactoryRepository factoryRepository;

    @Mock
    private ProductionLineRepository lineRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RoutingRepository routingRepository;

    @Mock
    private ProductionOrderRepository orderRepository;

    private PlanningDataImportPreviewService service;

    @BeforeEach
    void setUp() {
        service = new PlanningDataImportPreviewService(
                factoryRepository,
                lineRepository,
                machineRepository,
                productRepository,
                routingRepository,
                orderRepository
        );
        emptyDatabase();
    }

    @Test
    void previewsCompletePlanningDataWithoutPersisting() {
        String csv = csv(
                row(Map.of(
                        "type", "FACTORY",
                        "factoryCode", "factory-01",
                        "name", "서울 공장"
                )),
                row(Map.of(
                        "type", "PRODUCTION_LINE",
                        "factoryCode", "factory-01",
                        "lineCode", "line-01",
                        "name", "조립 라인"
                )),
                row(Map.of(
                        "type", "MACHINE",
                        "factoryCode", "factory-01",
                        "lineCode", "line-01",
                        "machineCode", "machine-01",
                        "name", "조립기",
                        "status", "available"
                )),
                row(Map.of(
                        "type", "PRODUCT",
                        "productCode", "product-01",
                        "name", "완제품",
                        "unit", "piece"
                )),
                routingRow(10, "CUT", "절단"),
                routingRow(20, "ASSEMBLE", "조립"),
                row(Map.of(
                        "type", "PRODUCTION_ORDER",
                        "productCode", "product-01",
                        "routingCode", "routing-01",
                        "orderNumber", "po-001",
                        "quantity", "10",
                        "releaseAt", "2026-08-03T08:00:00+09:00",
                        "dueAt", "2026-08-04T18:00:00+09:00",
                        "priority", "80"
                ))
        );

        PlanningDataImportPreviewResponse response =
                service.preview(file(csv));

        assertThat(response.readyToApply()).isTrue();
        assertThat(response.totalRows()).isEqualTo(7);
        assertThat(response.validRows()).isEqualTo(7);
        assertThat(response.invalidRows()).isZero();
        assertThat(response.rows().getFirst().normalizedValues())
                .containsEntry("factoryCode", "FACTORY-01");
        assertThat(response.rows().get(4).normalizedValues())
                .containsEntry("operationCode", "CUT");
    }

    @Test
    void bundledTemplatePassesTheSamePreviewValidation() throws Exception {
        byte[] template = new ClassPathResource(
                "static/planning-data-template.csv"
        ).getContentAsByteArray();

        PlanningDataImportPreviewResponse response = service.preview(
                new MockMultipartFile(
                        "file",
                        "planning-data-template.csv",
                        "text/csv",
                        template
                )
        );

        assertThat(response.readyToApply()).isTrue();
        assertThat(response.totalRows()).isEqualTo(6);
    }

    @Test
    void reportsReferenceOrderAndRowValueErrors() {
        String csv = csv(
                row(Map.of(
                        "type", "PRODUCTION_LINE",
                        "factoryCode", "MISSING",
                        "lineCode", "LINE-01",
                        "name", "라인"
                )),
                row(Map.of(
                        "type", "FACTORY",
                        "factoryCode", "FACTORY-01",
                        "name", "공장"
                )),
                row(Map.of(
                        "type", "PRODUCTION_ORDER",
                        "productCode", "PRODUCT-01",
                        "routingCode", "ROUTING-01",
                        "orderNumber", "PO-001",
                        "quantity", "0",
                        "releaseAt", "not-a-date",
                        "dueAt", "2026-08-04T18:00:00+09:00",
                        "priority", "101"
                ))
        );

        PlanningDataImportPreviewResponse response =
                service.preview(file(csv));

        assertThat(response.readyToApply()).isFalse();
        assertThat(response.invalidRows()).isEqualTo(3);
        assertThat(response.rows().get(0).errors())
                .extracting(PlanningDataImportRowError::code)
                .contains("REFERENCE_NOT_FOUND");
        assertThat(response.rows().get(1).errors())
                .extracting(PlanningDataImportRowError::code)
                .contains("REFERENCE_ORDER_INVALID");
        assertThat(response.rows().get(2).errors()).hasSize(3);
    }

    @Test
    void allowsOrderToReferenceExistingRouting() {
        Factory factory = Factory.create("FACTORY-DB", "기존 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-DB", "기존 라인");
        Machine machine =
                Machine.create(line, "MACHINE-DB", "기존 설비");
        Product product =
                Product.create("PRODUCT-DB", "기존 품목", ProductUnit.PIECE);
        Routing routing =
                Routing.create(product, "ROUTING-DB", "기존 Routing");
        routing.addOperation(10, "PROCESS", "가공", 30, machine);
        when(factoryRepository.findAll()).thenReturn(List.of(factory));
        when(lineRepository.findAll()).thenReturn(List.of(line));
        when(machineRepository.findAll()).thenReturn(List.of(machine));
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(routingRepository.findAll()).thenReturn(List.of(routing));

        String csv = csv(row(Map.of(
                "type", "PRODUCTION_ORDER",
                "productCode", "product-db",
                "routingCode", "routing-db",
                "orderNumber", "po-new",
                "quantity", "1",
                "releaseAt", OffsetDateTime.parse(
                        "2026-08-03T08:00:00+09:00"
                ).toString(),
                "dueAt", "2026-08-03T18:00:00+09:00",
                "priority", "50"
        )));

        PlanningDataImportPreviewResponse response =
                service.preview(file(csv));

        assertThat(response.readyToApply()).isTrue();
        assertThat(response.validRows()).isEqualTo(1);
    }

    @Test
    void rejectsFilesOverTwoMegabytesBeforeReadingRepositories() {
        byte[] oversized = new byte[
                (int) PlanningDataImportPreviewService.MAX_FILE_SIZE_BYTES + 1
        ];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oversized.csv",
                "text/csv",
                oversized
        );

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> {
                            assertThat(exception.errorCode())
                                    .isEqualTo(ErrorCode.INVALID_REQUEST);
                            assertThat(exception.getMessage())
                                    .contains("2MB");
                        }
                );
    }

    private void emptyDatabase() {
        lenient().when(factoryRepository.findAll()).thenReturn(List.of());
        lenient().when(lineRepository.findAll()).thenReturn(List.of());
        lenient().when(machineRepository.findAll()).thenReturn(List.of());
        lenient().when(productRepository.findAll()).thenReturn(List.of());
        lenient().when(routingRepository.findAll()).thenReturn(List.of());
        lenient().when(orderRepository.findAll()).thenReturn(List.of());
    }

    private String routingRow(
            int sequence,
            String operationCode,
            String operationName
    ) {
        return row(Map.ofEntries(
                Map.entry("type", "ROUTING"),
                Map.entry("factoryCode", "factory-01"),
                Map.entry("lineCode", "line-01"),
                Map.entry("machineCode", "machine-01"),
                Map.entry("productCode", "product-01"),
                Map.entry("routingCode", "routing-01"),
                Map.entry("name", "표준 Routing"),
                Map.entry("operationSequence", Integer.toString(sequence)),
                Map.entry("operationCode", operationCode),
                Map.entry("operationName", operationName),
                Map.entry("processingTimeMinutes", "30")
        ));
    }

    private String csv(String... rows) {
        return PlanningDataCsvTestSupport.header()
                + "\n"
                + String.join("\n", rows);
    }

    private String row(Map<String, String> fields) {
        return PlanningDataCsvTestSupport.row(fields);
    }

    private MockMultipartFile file(String csv) {
        return new MockMultipartFile(
                "file",
                "planning-data.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
    }
}
