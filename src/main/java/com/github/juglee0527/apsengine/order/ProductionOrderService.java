package com.github.juglee0527.apsengine.order;

import java.time.OffsetDateTime;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionOrderService {

    private final RoutingRepository routingRepository;
    private final ProductionOrderRepository productionOrderRepository;

    public ProductionOrderService(
            RoutingRepository routingRepository,
            ProductionOrderRepository productionOrderRepository
    ) {
        this.routingRepository = routingRepository;
        this.productionOrderRepository = productionOrderRepository;
    }

    @Transactional
    public ProductionOrder create(
            String orderNumber,
            long routingId,
            long quantity,
            OffsetDateTime releaseAt,
            OffsetDateTime dueAt,
            int priority
    ) {
        Routing routing = routingRepository.findDetailById(routingId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.ROUTING_NOT_FOUND));
        ProductionOrder order;
        try {
            order = ProductionOrder.create(
                    routing,
                    orderNumber,
                    quantity,
                    releaseAt,
                    dueAt,
                    priority
            );
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }

        if (productionOrderRepository.existsByOrderNumber(
                order.orderNumber()
        )) {
            throw new ApplicationException(
                    ErrorCode.PRODUCTION_ORDER_NUMBER_DUPLICATED
            );
        }

        try {
            return productionOrderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException exception) {
            ErrorCode errorCode =
                    ErrorCode.PRODUCTION_ORDER_NUMBER_DUPLICATED;
            throw new ApplicationException(
                    errorCode,
                    errorCode.defaultMessage(),
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public ProductionOrder getById(long productionOrderId) {
        validateId(productionOrderId);
        return productionOrderRepository.findById(productionOrderId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.PRODUCTION_ORDER_NOT_FOUND
                ));
    }

    @Transactional(readOnly = true)
    public Page<ProductionOrder> getPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "페이지 조건이 올바르지 않습니다."
            );
        }
        return productionOrderRepository.findAllBy(PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        ));
    }

    @Transactional
    public ProductionOrder confirm(long productionOrderId) {
        ProductionOrder order = getById(productionOrderId);
        try {
            order.confirm();
        } catch (IllegalStateException exception) {
            throw new ApplicationException(
                    ErrorCode.PRODUCTION_ORDER_STATUS_INVALID,
                    exception.getMessage(),
                    exception
            );
        }
        return order;
    }

    private void validateId(long productionOrderId) {
        if (productionOrderId < 1) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "생산오더 ID는 1 이상이어야 합니다."
            );
        }
    }
}
