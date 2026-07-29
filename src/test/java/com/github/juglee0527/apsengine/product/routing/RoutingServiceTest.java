package com.github.juglee0527.apsengine.product.routing;

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
class RoutingServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private RoutingRepository routingRepository;

    @InjectMocks
    private RoutingService routingService;

    @Test
    void createsRoutingWithOperations() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        Machine machine = machine();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(routingRepository.existsByProduct_IdAndCode(
                1L,
                "ROUTING-01"
        )).thenReturn(false);
        when(machineRepository.findById(10L))
                .thenReturn(Optional.of(machine));
        when(routingRepository.saveAndFlush(any(Routing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Routing routing = routingService.create(
                1L,
                "routing-01",
                "표준 Routing",
                List.of(operationRequest(10, "CUT"))
        );

        assertThat(routing.operations()).hasSize(1);
        assertThat(routing.operations().getFirst().machine())
                .isSameAs(machine);
        verify(routingRepository).saveAndFlush(routing);
    }

    @Test
    void rejectsDuplicatedOperationSequence() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertErrorCode(
                () -> routingService.create(
                        1L,
                        "ROUTING-01",
                        "표준 Routing",
                        List.of(
                                operationRequest(10, "CUT"),
                                operationRequest(10, "ASSEMBLY")
                        )
                ),
                ErrorCode.INVALID_REQUEST
        );

        verify(routingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingMachine() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(routingRepository.existsByProduct_IdAndCode(
                1L,
                "ROUTING-01"
        )).thenReturn(false);
        when(machineRepository.findById(10L)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> routingService.create(
                        1L,
                        "ROUTING-01",
                        "표준 Routing",
                        List.of(operationRequest(10, "CUT"))
                ),
                ErrorCode.MACHINE_NOT_FOUND
        );
    }

    private OperationCreateRequest operationRequest(
            int sequence,
            String code
    ) {
        return new OperationCreateRequest(
                sequence,
                code,
                "공정",
                30,
                10L
        );
    }

    private Machine machine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, "MACHINE-01", "가공 설비");
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
}
