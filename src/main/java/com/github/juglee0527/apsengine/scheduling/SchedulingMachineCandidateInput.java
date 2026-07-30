package com.github.juglee0527.apsengine.scheduling;

import java.util.List;

import com.github.juglee0527.apsengine.capacity.UnavailableInterval;
import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;

public record SchedulingMachineCandidateInput(
        long machineId,
        int priority,
        List<WeeklyWorkingTime> workingTimes,
        List<UnavailableInterval> unavailableIntervals
) {

    public SchedulingMachineCandidateInput {
        if (machineId < 1) {
            throw new IllegalArgumentException(
                    "후보 설비 식별자는 1 이상이어야 합니다."
            );
        }
        if (priority < 1) {
            throw new IllegalArgumentException(
                    "후보 설비 우선순위는 1 이상이어야 합니다."
            );
        }
        workingTimes = workingTimes == null
                ? List.of()
                : List.copyOf(workingTimes);
        unavailableIntervals = unavailableIntervals == null
                ? List.of()
                : List.copyOf(unavailableIntervals);
    }
}
