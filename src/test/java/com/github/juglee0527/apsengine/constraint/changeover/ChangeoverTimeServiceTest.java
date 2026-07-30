package com.github.juglee0527.apsengine.constraint.changeover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.ProductUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangeoverTimeServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ChangeoverTimeRepository changeoverTimeRepository;

    @InjectMocks
    private ChangeoverTimeService changeoverTimeService;

    @Test
    void createsChangeoverTime() {
        Machine machine = machine();
        Product fromProduct = product("PRODUCT-A");
        Product toProduct = product("PRODUCT-B");
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine));
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(fromProduct));
        when(productRepository.findById(20L))
                .thenReturn(Optional.of(toProduct));
        when(changeoverTimeRepository
                .existsByMachine_IdAndFromProduct_IdAndToProduct_Id(
                        1L,
                        10L,
                        20L
                )).thenReturn(false);
        when(changeoverTimeRepository.saveAndFlush(any(ChangeoverTime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeoverTime result = changeoverTimeService.create(
                1L,
                10L,
                20L,
                30
        );

        assertThat(result.machine()).isSameAs(machine);
        assertThat(result.fromProduct()).isSameAs(fromProduct);
        assertThat(result.toProduct()).isSameAs(toProduct);
        assertThat(result.changeoverMinutes()).isEqualTo(30);
    }

    @Test
    void rejectsDuplicatedDirectionalMapping() {
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine()));
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product("PRODUCT-A")));
        when(productRepository.findById(20L))
                .thenReturn(Optional.of(product("PRODUCT-B")));
        when(changeoverTimeRepository
                .existsByMachine_IdAndFromProduct_IdAndToProduct_Id(
                        1L,
                        10L,
                        20L
                )).thenReturn(true);

        assertErrorCode(
                () -> changeoverTimeService.create(1L, 10L, 20L, 30),
                ErrorCode.CHANGEOVER_TIME_DUPLICATED
        );

        verify(changeoverTimeRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSameProductBeforeLoadingReferences() {
        assertErrorCode(
                () -> changeoverTimeService.create(1L, 10L, 10L, 0),
                ErrorCode.INVALID_REQUEST
        );

        verify(machineRepository, never()).findById(any());
        verify(productRepository, never()).findById(any());
    }

    @Test
    void returnsZeroForSameProductWithoutRepositoryLookup() {
        int minutes = changeoverTimeService.resolveMinutes(1L, 10L, 10L);

        assertThat(minutes).isZero();
        verify(changeoverTimeRepository, never())
                .findByMachine_IdAndFromProduct_IdAndToProduct_IdAndActiveTrue(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void returnsZeroWhenDirectionalMappingIsMissing() {
        when(changeoverTimeRepository
                .findByMachine_IdAndFromProduct_IdAndToProduct_IdAndActiveTrue(
                        1L,
                        10L,
                        20L
                )).thenReturn(Optional.empty());

        int minutes = changeoverTimeService.resolveMinutes(1L, 10L, 20L);

        assertThat(minutes).isZero();
    }

    @Test
    void resolvesOnlyRequestedDirection() {
        ChangeoverTime forward = ChangeoverTime.create(
                machine(),
                product("PRODUCT-A"),
                product("PRODUCT-B"),
                30
        );
        when(changeoverTimeRepository
                .findByMachine_IdAndFromProduct_IdAndToProduct_IdAndActiveTrue(
                        1L,
                        10L,
                        20L
                )).thenReturn(Optional.of(forward));
        when(changeoverTimeRepository
                .findByMachine_IdAndFromProduct_IdAndToProduct_IdAndActiveTrue(
                        1L,
                        20L,
                        10L
                )).thenReturn(Optional.empty());

        assertThat(changeoverTimeService.resolveMinutes(1L, 10L, 20L))
                .isEqualTo(30);
        assertThat(changeoverTimeService.resolveMinutes(1L, 20L, 10L))
                .isZero();
    }

    @Test
    void getsMappingsOnlyForExistingMachine() {
        ChangeoverTime changeoverTime = ChangeoverTime.create(
                machine(),
                product("PRODUCT-A"),
                product("PRODUCT-B"),
                30
        );
        when(machineRepository.existsById(1L)).thenReturn(true);
        when(changeoverTimeRepository
                .findAllByMachine_IdAndActiveTrueOrderByFromProduct_IdAscToProduct_IdAsc(
                        1L
                )).thenReturn(List.of(changeoverTime));

        List<ChangeoverTime> result =
                changeoverTimeService.getAllByMachine(1L);

        assertThat(result).containsExactly(changeoverTime);
    }

    @Test
    void getsActiveChangeoverTimeById() {
        ChangeoverTime changeoverTime = ChangeoverTime.create(
                machine(),
                product("PRODUCT-A"),
                product("PRODUCT-B"),
                30
        );
        when(changeoverTimeRepository.findActiveDetailById(1L))
                .thenReturn(Optional.of(changeoverTime));

        ChangeoverTime result = changeoverTimeService.getById(1L);

        assertThat(result).isSameAs(changeoverTime);
    }

    @Test
    void rejectsMissingChangeoverTime() {
        when(changeoverTimeRepository.findActiveDetailById(1L))
                .thenReturn(Optional.empty());

        assertErrorCode(
                () -> changeoverTimeService.getById(1L),
                ErrorCode.CHANGEOVER_TIME_NOT_FOUND
        );
    }

    private void assertErrorCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ErrorCode expected
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(expected)
                );
    }

    private Machine machine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, "MACHINE-01", "가공 설비");
    }

    private Product product(String code) {
        return Product.create(code, code, ProductUnit.PIECE);
    }
}
