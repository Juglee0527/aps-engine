package com.github.juglee0527.apsengine.product.routing;

import java.util.ArrayList;
import java.util.List;

public record RoutingResponse(
        Long id,
        Long productId,
        String code,
        String name,
        boolean active,
        List<OperationResponse> operations
) {

    public RoutingResponse {
        operations = List.copyOf(operations);
    }

    public static RoutingResponse from(Routing routing) {
        List<OperationResponse> operations =
                new ArrayList<>(routing.operations().size());
        for (Operation operation : routing.operations()) {
            operations.add(OperationResponse.from(operation));
        }
        return new RoutingResponse(
                routing.id(),
                routing.product().id(),
                routing.code(),
                routing.name(),
                routing.isActive(),
                operations
        );
    }
}
