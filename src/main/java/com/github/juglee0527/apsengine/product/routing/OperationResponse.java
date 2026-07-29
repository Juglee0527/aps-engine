package com.github.juglee0527.apsengine.product.routing;

public record OperationResponse(
        Long id,
        int sequence,
        String code,
        String name,
        int processingTimeMinutes,
        Long machineId
) {

    public static OperationResponse from(Operation operation) {
        return new OperationResponse(
                operation.id(),
                operation.sequence(),
                operation.code(),
                operation.name(),
                operation.processingTimeMinutes(),
                operation.machine().id()
        );
    }
}
