package com.github.juglee0527.apsengine.product.routing;

import com.github.juglee0527.apsengine.machine.MachineStatus;

public record OperationMachineCandidateResponse(
        Long machineId,
        int priority,
        MachineStatus status
) {

    public static OperationMachineCandidateResponse from(
            OperationMachineCandidate candidate
    ) {
        return new OperationMachineCandidateResponse(
                candidate.machine().id(),
                candidate.priority(),
                candidate.machine().status()
        );
    }
}
