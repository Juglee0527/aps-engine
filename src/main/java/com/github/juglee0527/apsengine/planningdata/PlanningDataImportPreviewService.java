package com.github.juglee0527.apsengine.planningdata;

import java.io.IOException;
import java.util.List;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PlanningDataImportPreviewService {

    public static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;
    public static final int MAX_DATA_ROWS = 2_000;

    private final FactoryRepository factoryRepository;
    private final ProductionLineRepository productionLineRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final RoutingRepository routingRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final PlanningDataCsvParser parser =
            new PlanningDataCsvParser();
    private final PlanningDataImportValidator validator =
            new PlanningDataImportValidator();

    public PlanningDataImportPreviewService(
            FactoryRepository factoryRepository,
            ProductionLineRepository productionLineRepository,
            MachineRepository machineRepository,
            ProductRepository productRepository,
            RoutingRepository routingRepository,
            ProductionOrderRepository productionOrderRepository
    ) {
        this.factoryRepository = factoryRepository;
        this.productionLineRepository = productionLineRepository;
        this.machineRepository = machineRepository;
        this.productRepository = productRepository;
        this.routingRepository = routingRepository;
        this.productionOrderRepository = productionOrderRepository;
    }

    @Transactional(readOnly = true)
    public PlanningDataImportPreviewResponse preview(MultipartFile file) {
        validateFile(file);
        try {
            PlanningDataCsvParser.ParsedCsv csv = parser.parse(
                    file.getBytes(),
                    MAX_DATA_ROWS
            );
            ExistingPlanningData existing = ExistingPlanningData.from(
                    factoryRepository.findAll(),
                    productionLineRepository.findAll(),
                    machineRepository.findAll(),
                    productRepository.findAll(),
                    routingRepository.findAll(),
                    productionOrderRepository.findAll()
            );
            List<PlanningDataImportRowPreview> rows =
                    validator.validate(csv, existing);
            return PlanningDataImportPreviewResponse.from(rows);
        } catch (IOException exception) {
            throw invalidFile(
                    "CSV 파일을 읽을 수 없습니다.",
                    exception
            );
        } catch (IllegalArgumentException exception) {
            throw invalidFile(exception.getMessage(), exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidFile("CSV 파일은 필수입니다.", null);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw invalidFile(
                    "CSV 파일은 2MB를 초과할 수 없습니다.",
                    null
            );
        }
    }

    private ApplicationException invalidFile(
            String message,
            Throwable cause
    ) {
        return new ApplicationException(
                ErrorCode.INVALID_REQUEST,
                message,
                cause
        );
    }
}
