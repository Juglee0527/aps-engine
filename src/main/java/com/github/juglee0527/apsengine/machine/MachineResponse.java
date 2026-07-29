package com.github.juglee0527.apsengine.machine;

public record MachineResponse(
        Long id,
        Long productionLineId,
        String code,
        String name,
        MachineStatus status
) {

    public static MachineResponse from(Machine machine) {
        return new MachineResponse(
                machine.id(),
                machine.productionLine().id(),
                machine.code(),
                machine.name(),
                machine.status()
        );
    }
}

