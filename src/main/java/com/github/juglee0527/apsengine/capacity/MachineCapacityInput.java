package com.github.juglee0527.apsengine.capacity;

record MachineCapacityInput(
        long machineId,
        String machineCode,
        String machineName,
        long availableMinutes,
        long loadMinutes
) {

    MachineCapacityInput {
        if (machineId < 1) {
            throw new IllegalArgumentException(
                    "설비 식별자는 1 이상이어야 합니다."
            );
        }
        if (machineCode == null || machineCode.isBlank()
                || machineName == null || machineName.isBlank()) {
            throw new IllegalArgumentException(
                    "설비 코드와 이름은 필수입니다."
            );
        }
        if (availableMinutes < 0 || loadMinutes < 0) {
            throw new IllegalArgumentException(
                    "가용시간과 부하는 0분 이상이어야 합니다."
            );
        }
    }
}
