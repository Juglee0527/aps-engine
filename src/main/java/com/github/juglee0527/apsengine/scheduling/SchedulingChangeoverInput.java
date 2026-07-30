package com.github.juglee0527.apsengine.scheduling;

public record SchedulingChangeoverInput(
        long machineId,
        long fromProductId,
        long toProductId,
        int changeoverMinutes
) {

    public SchedulingChangeoverInput {
        if (machineId < 1 || fromProductId < 1 || toProductId < 1) {
            throw new IllegalArgumentException(
                    "설비와 품목 식별자는 1 이상이어야 합니다."
            );
        }
        if (fromProductId == toProductId) {
            throw new IllegalArgumentException(
                    "동일 품목 전환시간은 입력할 수 없습니다."
            );
        }
        if (changeoverMinutes < 0) {
            throw new IllegalArgumentException(
                    "Changeover Time은 0분 이상이어야 합니다."
            );
        }
    }
}
