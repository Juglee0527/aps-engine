package com.github.juglee0527.apsengine.factory.line;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionLineService {

    private final FactoryRepository factoryRepository;
    private final ProductionLineRepository productionLineRepository;

    public ProductionLineService(
            FactoryRepository factoryRepository,
            ProductionLineRepository productionLineRepository
    ) {
        this.factoryRepository = factoryRepository;
        this.productionLineRepository = productionLineRepository;
    }

    @Transactional
    public ProductionLine create(long factoryId, String code, String name) {
        Factory factory = getActiveFactory(factoryId);
        ProductionLine productionLine =
                ProductionLine.create(factory, code, name);

        validateCodeDuplication(factoryId, productionLine.code());

        try {
            return productionLineRepository.saveAndFlush(productionLine);
        } catch (DataIntegrityViolationException exception) {
            ErrorCode errorCode =
                    ErrorCode.PRODUCTION_LINE_CODE_DUPLICATED;
            throw new ApplicationException(
                    errorCode,
                    errorCode.defaultMessage(),
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductionLine> getPageByFactory(
            long factoryId,
            int page,
            int size
    ) {
        validateFactoryId(factoryId);
        validatePage(page, size);
        if (!factoryRepository.existsById(factoryId)) {
            throw new ApplicationException(ErrorCode.FACTORY_NOT_FOUND);
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );
        return productionLineRepository.findAllByFactory_Id(
                factoryId,
                pageRequest
        );
    }

    private Factory getActiveFactory(long factoryId) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.FACTORY_NOT_FOUND));

        if (!factory.isActive()) {
            throw new ApplicationException(ErrorCode.FACTORY_INACTIVE);
        }
        return factory;
    }

    private void validateCodeDuplication(long factoryId, String code) {
        if (productionLineRepository.existsByFactory_IdAndCode(
                factoryId,
                code
        )) {
            throw new ApplicationException(
                    ErrorCode.PRODUCTION_LINE_CODE_DUPLICATED
            );
        }
    }

    private void validateFactoryId(long factoryId) {
        if (factoryId < 1) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "공장 ID는 1 이상이어야 합니다."
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
