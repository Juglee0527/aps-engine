package com.github.juglee0527.apsengine.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductionOrderServiceTest {

    private static final OffsetDateTime RELEASE_AT =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
    private static final OffsetDateTime DUE_AT =
            OffsetDateTime.parse("2026-08-04T18:00:00+09:00");

    @Mock
    private RoutingRepository routingRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @InjectMocks
    private ProductionOrderService productionOrderService;

    @Test
    void createsProductionOrder() {
        when(routingRepository.findDetailById(1L))
                .thenReturn(Optional.of(routing()));
        when(productionOrderRepository.existsByOrderNumber("PO-001"))
                .thenReturn(false);
        when(productionOrderRepository.saveAndFlush(
                any(ProductionOrder.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        ProductionOrder order = productionOrderService.create(
                "po-001",
                1L,
                10,
                RELEASE_AT,
                DUE_AT,
                80
        );

        assertThat(order.orderNumber()).isEqualTo("PO-001");
        assertThat(order.priority()).isEqualTo(80);
        verify(productionOrderRepository).saveAndFlush(order);
    }

    @Test
    void rejectsDuplicatedOrderNumber() {
        when(routingRepository.findDetailById(1L))
                .thenReturn(Optional.of(routing()));
        when(productionOrderRepository.existsByOrderNumber("PO-001"))
                .thenReturn(true);

        assertErrorCode(
                () -> productionOrderService.create(
                        "PO-001",
                        1L,
                        10,
                        RELEASE_AT,
                        DUE_AT,
                        80
                ),
                ErrorCode.PRODUCTION_ORDER_NUMBER_DUPLICATED
        );

        verify(productionOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void confirmsDraftOrder() {
        ProductionOrder order = order();
        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        ProductionOrder result = productionOrderService.confirm(1L);

        assertThat(result.status())
                .isEqualTo(ProductionOrderStatus.CONFIRMED);
    }

    @Test
    void rejectsInvalidStatusTransition() {
        ProductionOrder order = order();
        order.confirm();
        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertErrorCode(
                () -> productionOrderService.confirm(1L),
                ErrorCode.PRODUCTION_ORDER_STATUS_INVALID
        );
    }

    private ProductionOrder order() {
        return ProductionOrder.create(
                routing(),
                "PO-001",
                10,
                RELEASE_AT,
                DUE_AT,
                80
        );
    }

    private Routing routing() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        Routing routing =
                Routing.create(product, "ROUTING-01", "표준 Routing");
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        Machine machine = Machine.create(line, "MACHINE-01", "가공 설비");
        routing.addOperation(10, "CUT", "절단", 15, machine);
        return routing;
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
