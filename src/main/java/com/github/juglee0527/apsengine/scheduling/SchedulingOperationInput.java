package com.github.juglee0527.apsengine.scheduling;

import java.util.List;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;

public record SchedulingOperationInput(
        long operationId,
        long machineId,
        int sequence,
        String operationCode,
        String operationName,
        long processingTimeMinutesPerUnit,
        List<WeeklyWorkingTime> workingTimes
) {

    public SchedulingOperationInput {
        if (operationId < 1 || machineId < 1) {
            throw new IllegalArgumentException(
                    "공정과 설비 식별자는 1 이상이어야 합니다."
            );
        }
        if (sequence < 1) {
            throw new IllegalArgumentException(
                    "공정 순서는 1 이상이어야 합니다."
            );
        }
        if (processingTimeMinutesPerUnit < 1) {
            throw new IllegalArgumentException(
                    "단위 처리시간은 1분 이상이어야 합니다."
            );
        }
        workingTimes = workingTimes == null
                ? List.of()
                : List.copyOf(workingTimes);
    }
}
