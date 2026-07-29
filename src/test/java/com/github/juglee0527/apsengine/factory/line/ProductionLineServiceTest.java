package com.github.juglee0527.apsengine.factory.line;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ProductionLineServiceTest {

    @Mock
    private FactoryRepository factoryRepository;

    @Mock
    private ProductionLineRepository productionLineRepository;

    @InjectMocks
    private ProductionLineService productionLineService;

    @Test
    void createsProductionLineInFactory() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        when(factoryRepository.findById(1L)).thenReturn(Optional.of(factory));
        when(productionLineRepository.existsByFactory_IdAndCode(
                1L,
                "LINE-01"
        )).thenReturn(false);
        when(productionLineRepository.saveAndFlush(any(ProductionLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductionLine productionLine = productionLineService.create(
                1L,
                "line-01",
                "조립 라인"
        );

        assertThat(productionLine.factory()).isSameAs(factory);
        assertThat(productionLine.code()).isEqualTo("LINE-01");
        verify(productionLineRepository).saveAndFlush(productionLine);
    }

    @Test
    void rejectsMissingFactory() {
        when(factoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> productionLineService.create(
                        999L,
                        "LINE-01",
                        "조립 라인"
                ),
                ErrorCode.FACTORY_NOT_FOUND
        );

        verify(productionLineRepository, never())
                .saveAndFlush(any(ProductionLine.class));
    }

    @Test
    void rejectsInactiveFactory() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        factory.deactivate();
        when(factoryRepository.findById(1L)).thenReturn(Optional.of(factory));

        assertErrorCode(
                () -> productionLineService.create(
                        1L,
                        "LINE-01",
                        "조립 라인"
                ),
                ErrorCode.FACTORY_INACTIVE
        );
    }

    @Test
    void rejectsDuplicatedCodeInSameFactory() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        when(factoryRepository.findById(1L)).thenReturn(Optional.of(factory));
        when(productionLineRepository.existsByFactory_IdAndCode(
                1L,
                "LINE-01"
        )).thenReturn(true);

        assertErrorCode(
                () -> productionLineService.create(
                        1L,
                        "line-01",
                        "조립 라인"
                ),
                ErrorCode.PRODUCTION_LINE_CODE_DUPLICATED
        );
    }

    @Test
    void getsProductionLinePageByFactory() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine productionLine =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        PageRequest pageRequest = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );
        Page<ProductionLine> expectedPage =
                new PageImpl<>(List.of(productionLine), pageRequest, 1);
        when(factoryRepository.existsById(1L)).thenReturn(true);
        when(productionLineRepository.findAllByFactory_Id(1L, pageRequest))
                .thenReturn(expectedPage);

        Page<ProductionLine> result =
                productionLineService.getPageByFactory(1L, 0, 20);

        assertThat(result).isSameAs(expectedPage);
    }

    @Test
    void rejectsMissingFactoryWhenGettingPage() {
        when(factoryRepository.existsById(999L)).thenReturn(false);

        assertErrorCode(
                () -> productionLineService.getPageByFactory(999L, 0, 20),
                ErrorCode.FACTORY_NOT_FOUND
        );

        verify(productionLineRepository, never())
                .findAllByFactory_Id(any(), any());
    }

    private void assertErrorCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }
}
