package com.github.juglee0527.apsengine.product.routing;

import java.util.List;

public record OperationResponse(
        Long id,
        int sequence,
        String code,
        String name,
        int processingTimeMinutes,
        Long machineId,
        List<OperationMachineCandidateResponse> machineCandidates
) {

    public OperationResponse {
        machineCandidates = List.copyOf(machineCandidates);
    }

    public static OperationResponse from(Operation operation) {
        return new OperationResponse(
                operation.id(),
                operation.sequence(),
                operation.code(),
                operation.name(),
                operation.processingTimeMinutes(),
                operation.machine().id(),
                operation.machineCandidates().stream()
                        .map(OperationMachineCandidateResponse::from)
                        .toList()
        );
    }
}
