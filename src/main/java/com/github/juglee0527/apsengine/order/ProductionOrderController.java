package com.github.juglee0527.apsengine.order;

import java.net.URI;

import com.github.juglee0527.apsengine.common.web.PageResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/production-orders")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    public ProductionOrderController(
            ProductionOrderService productionOrderService
    ) {
        this.productionOrderService = productionOrderService;
    }

    @PostMapping
    public ResponseEntity<ProductionOrderResponse> create(
            @Valid @RequestBody ProductionOrderCreateRequest request
    ) {
        ProductionOrder order = productionOrderService.create(
                request.orderNumber(),
                request.routingId(),
                request.quantity(),
                request.releaseAt(),
                request.dueAt(),
                request.priority()
        );
        ProductionOrderResponse response =
                ProductionOrderResponse.from(order);
        URI location =
                URI.create("/api/v1/production-orders/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{productionOrderId}")
    public ProductionOrderResponse getById(
            @PathVariable long productionOrderId
    ) {
        return ProductionOrderResponse.from(
                productionOrderService.getById(productionOrderId)
        );
    }

    @GetMapping
    public PageResponse<ProductionOrderResponse> getPage(
            @Valid @ModelAttribute ProductionOrderSearchParameters request
    ) {
        Page<ProductionOrder> page = productionOrderService.search(request);
        return PageResponse.from(page, ProductionOrderResponse::from);
    }

    @PostMapping("/{productionOrderId}/confirm")
    public ProductionOrderResponse confirm(
            @PathVariable long productionOrderId
    ) {
        return ProductionOrderResponse.from(
                productionOrderService.confirm(productionOrderId)
        );
    }
}
