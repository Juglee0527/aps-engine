package com.github.juglee0527.apsengine.factory;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FactoryService {

    private final FactoryRepository factoryRepository;

    public FactoryService(FactoryRepository factoryRepository) {
        this.factoryRepository = factoryRepository;
    }

    @Transactional
    public Factory create(String code, String name) {
        Factory factory = Factory.create(code, name);
        validateCodeDuplication(factory.code());

        try {
            return factoryRepository.saveAndFlush(factory);
        } catch (DataIntegrityViolationException exception) {
            throw duplicatedFactoryCodeException(exception);
        }
    }

    private void validateCodeDuplication(String normalizedCode) {
        if (factoryRepository.existsByCode(normalizedCode)) {
            throw duplicatedFactoryCodeException();
        }
    }

    private ApplicationException duplicatedFactoryCodeException() {
        return new ApplicationException(ErrorCode.FACTORY_CODE_DUPLICATED);
    }

    private ApplicationException duplicatedFactoryCodeException(
            DataIntegrityViolationException cause
    ) {
        ErrorCode errorCode = ErrorCode.FACTORY_CODE_DUPLICATED;
        return new ApplicationException(
                errorCode,
                errorCode.defaultMessage(),
                cause
        );
    }
}
