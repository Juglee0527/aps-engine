package com.github.juglee0527.apsengine.constraint.maintenance;

import java.time.OffsetDateTime;

public record MachineMaintenanceResponse(
        Long id,
        Long machineId,
        String machineCode,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String reason,
        boolean active
) {

    public static MachineMaintenanceResponse from(
            MachineMaintenance maintenance
    ) {
        return new MachineMaintenanceResponse(
                maintenance.id(),
                maintenance.machine().id(),
                maintenance.machine().code(),
                maintenance.startAt(),
                maintenance.endAt(),
                maintenance.reason(),
                maintenance.isActive()
        );
    }
}
