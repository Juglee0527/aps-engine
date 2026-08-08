package com.github.juglee0527.apsengine.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ProductionOrderSearchParameters(
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        @Size(max = 100) String query,
        ProductionOrderStatus status
) {
    public ProductionOrderSearchParameters {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        query = query == null || query.isBlank()
                ? null
                : query.trim().toLowerCase();
    }
}
