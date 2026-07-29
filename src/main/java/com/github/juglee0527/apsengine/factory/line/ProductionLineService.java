package com.github.juglee0527.apsengine.factory.line;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;

import org.springframework.dao.DataIntegrityViolationException;
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
}

