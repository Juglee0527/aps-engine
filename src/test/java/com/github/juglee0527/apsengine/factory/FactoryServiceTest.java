package com.github.juglee0527.apsengine.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class FactoryServiceTest {

    @Mock
    private FactoryRepository factoryRepository;

    @InjectMocks
    private FactoryService factoryService;

    @Test
    void createsFactoryWithNormalizedCode() {
        when(factoryRepository.existsByCode("FACTORY-01")).thenReturn(false);
        when(factoryRepository.saveAndFlush(any(Factory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Factory createdFactory =
                factoryService.create("factory-01", "서울 공장");

        assertThat(createdFactory.code()).isEqualTo("FACTORY-01");
        assertThat(createdFactory.name()).isEqualTo("서울 공장");
        assertThat(createdFactory.isActive()).isTrue();
        verify(factoryRepository).saveAndFlush(createdFactory);
    }

    @Test
    void rejectsDuplicatedNormalizedCodeBeforeInsert() {
        when(factoryRepository.existsByCode("FACTORY-01")).thenReturn(true);

        assertThatThrownBy(
                () -> factoryService.create("factory-01", "서울 공장")
        )
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.FACTORY_CODE_DUPLICATED)
                );

        verify(factoryRepository, never()).saveAndFlush(any(Factory.class));
    }

    @Test
    void convertsUniqueConstraintRaceToDuplicatedCodeError() {
        when(factoryRepository.existsByCode("FACTORY-01")).thenReturn(false);
        when(factoryRepository.saveAndFlush(any(Factory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(
                () -> factoryService.create("factory-01", "서울 공장")
        )
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.FACTORY_CODE_DUPLICATED)
                )
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }
}

