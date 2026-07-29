package com.github.juglee0527.apsengine.factory;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Transactional(readOnly = true)
    public Factory getById(long factoryId) {
        if (factoryId < 1) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "공장 ID는 1 이상이어야 합니다."
            );
        }

        return factoryRepository.findById(factoryId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.FACTORY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<Factory> getPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "페이지 조건이 올바르지 않습니다."
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );
        return factoryRepository.findAll(pageRequest);
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
