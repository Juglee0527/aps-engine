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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

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

    @Test
    void getsFactoryById() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        when(factoryRepository.findById(1L)).thenReturn(Optional.of(factory));

        Factory foundFactory = factoryService.getById(1L);

        assertThat(foundFactory).isSameAs(factory);
    }

    @Test
    void rejectsInvalidFactoryId() {
        assertThatThrownBy(() -> factoryService.getById(0L))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST)
                );

        verify(factoryRepository, never()).findById(any());
    }

    @Test
    void throwsNotFoundWhenFactoryDoesNotExist() {
        when(factoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factoryService.getById(999L))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.FACTORY_NOT_FOUND)
                );
    }

    @Test
    void getsFactoryPageOrderedById() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        PageRequest expectedPageRequest = PageRequest.of(
                0,
                20,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC,
                        "id"
                )
        );
        Page<Factory> expectedPage = new PageImpl<>(
                List.of(factory),
                expectedPageRequest,
                1
        );
        when(factoryRepository.findAll(expectedPageRequest))
                .thenReturn(expectedPage);

        Page<Factory> result = factoryService.getPage(0, 20);

        assertThat(result).isSameAs(expectedPage);
    }

    @Test
    void rejectsInvalidFactoryPage() {
        assertThatThrownBy(() -> factoryService.getPage(-1, 101))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST)
                );

        verify(factoryRepository, never()).findAll(any(PageRequest.class));
    }
}
