package com.github.juglee0527.apsengine.machine;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineService {

    private final ProductionLineRepository productionLineRepository;
    private final MachineRepository machineRepository;

    public MachineService(
            ProductionLineRepository productionLineRepository,
            MachineRepository machineRepository
    ) {
        this.productionLineRepository = productionLineRepository;
        this.machineRepository = machineRepository;
    }

    @Transactional
    public Machine create(
            long productionLineId,
            String code,
            String name,
            MachineStatus status
    ) {
        validateProductionLineId(productionLineId);
        ProductionLine productionLine =
                getActiveProductionLine(productionLineId);
        Machine machine = Machine.create(
                productionLine,
                code,
                name,
                status
        );

        if (machineRepository.existsByProductionLine_IdAndCode(
                productionLineId,
                machine.code()
        )) {
            throw new ApplicationException(
                    ErrorCode.MACHINE_CODE_DUPLICATED
            );
        }

        try {
            return machineRepository.saveAndFlush(machine);
        } catch (DataIntegrityViolationException exception) {
            ErrorCode errorCode = ErrorCode.MACHINE_CODE_DUPLICATED;
            throw new ApplicationException(
                    errorCode,
                    errorCode.defaultMessage(),
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Machine getById(long machineId) {
        if (machineId < 1) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "설비 ID는 1 이상이어야 합니다."
            );
        }
        return machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.MACHINE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<Machine> getPageByProductionLine(
            long productionLineId,
            int page,
            int size
    ) {
        validateProductionLineId(productionLineId);
        validatePage(page, size);
        if (!productionLineRepository.existsById(productionLineId)) {
            throw new ApplicationException(
                    ErrorCode.PRODUCTION_LINE_NOT_FOUND
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );
        return machineRepository.findAllByProductionLine_Id(
                productionLineId,
                pageRequest
        );
    }

    private ProductionLine getActiveProductionLine(long productionLineId) {
        ProductionLine productionLine =
                productionLineRepository.findById(productionLineId)
                        .orElseThrow(() -> new ApplicationException(
                                ErrorCode.PRODUCTION_LINE_NOT_FOUND
                        ));

        if (!productionLine.isActive()) {
            throw new ApplicationException(
                    ErrorCode.PRODUCTION_LINE_INACTIVE
            );
        }
        return productionLine;
    }

    private void validateProductionLineId(long productionLineId) {
        if (productionLineId < 1) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "생산라인 ID는 1 이상이어야 합니다."
            );
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "페이지 조건이 올바르지 않습니다."
            );
        }
    }
}
