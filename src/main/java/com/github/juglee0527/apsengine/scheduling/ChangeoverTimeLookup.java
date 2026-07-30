package com.github.juglee0527.apsengine.scheduling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChangeoverTimeLookup {

    private final Map<ChangeoverKey, Integer> minutesByKey;

    private ChangeoverTimeLookup(
            Map<ChangeoverKey, Integer> minutesByKey
    ) {
        this.minutesByKey = Map.copyOf(minutesByKey);
    }

    static ChangeoverTimeLookup from(
            List<SchedulingChangeoverInput> inputs
    ) {
        if (inputs == null) {
            throw new IllegalArgumentException(
                    "Changeover Time 목록은 null일 수 없습니다."
            );
        }
        Map<ChangeoverKey, Integer> minutesByKey = new HashMap<>();
        for (SchedulingChangeoverInput input : inputs) {
            if (input == null) {
                throw new IllegalArgumentException(
                        "Changeover Time 입력은 null일 수 없습니다."
                );
            }
            ChangeoverKey key = new ChangeoverKey(
                    input.machineId(),
                    input.fromProductId(),
                    input.toProductId()
            );
            Integer previous = minutesByKey.putIfAbsent(
                    key,
                    input.changeoverMinutes()
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                        "중복된 Changeover Time 입력이 있습니다."
                );
            }
        }
        return new ChangeoverTimeLookup(minutesByKey);
    }

    int minutesFor(
            long machineId,
            long fromProductId,
            long toProductId
    ) {
        if (fromProductId == toProductId) {
            return 0;
        }
        return minutesByKey.getOrDefault(
                new ChangeoverKey(
                        machineId,
                        fromProductId,
                        toProductId
                ),
                0
        );
    }

    private record ChangeoverKey(
            long machineId,
            long fromProductId,
            long toProductId
    ) {
    }
}
