package com.github.juglee0527.apsengine.constraint.changeover;

public record ChangeoverTimeResponse(
        Long id,
        Long machineId,
        String machineCode,
        Long fromProductId,
        String fromProductCode,
        Long toProductId,
        String toProductCode,
        int changeoverMinutes,
        boolean active
) {

    public static ChangeoverTimeResponse from(ChangeoverTime changeoverTime) {
        return new ChangeoverTimeResponse(
                changeoverTime.id(),
                changeoverTime.machine().id(),
                changeoverTime.machine().code(),
                changeoverTime.fromProduct().id(),
                changeoverTime.fromProduct().code(),
                changeoverTime.toProduct().id(),
                changeoverTime.toProduct().code(),
                changeoverTime.changeoverMinutes(),
                changeoverTime.isActive()
        );
    }
}
