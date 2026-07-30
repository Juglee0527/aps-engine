package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

record FrozenScheduleSeed(
        List<ScheduledTask> tasks,
        Map<Long, OffsetDateTime> machineAvailableAt,
        Map<Long, Long> lastProductByMachine,
        Map<Long, OffsetDateTime> orderAvailableAt
) {

    FrozenScheduleSeed {
        tasks = List.copyOf(tasks);
        machineAvailableAt = Map.copyOf(machineAvailableAt);
        lastProductByMachine = Map.copyOf(lastProductByMachine);
        orderAvailableAt = Map.copyOf(orderAvailableAt);
    }

    static FrozenScheduleSeed empty() {
        return new FrozenScheduleSeed(
                List.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }
}
