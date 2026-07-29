package com.github.juglee0527.apsengine.scheduling;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRunRepository
        extends JpaRepository<ScheduleRun, Long> {

    @EntityGraph(attributePaths = {
            "scheduledOperations",
            "scheduledOperations.productionOrder",
            "scheduledOperations.productionOrder.routing",
            "scheduledOperations.productionOrder.routing.product",
            "scheduledOperations.operation",
            "scheduledOperations.machine"
    })
    Optional<ScheduleRun> findByExecutionKey(UUID executionKey);

    @Override
    @EntityGraph(attributePaths = {
            "scheduledOperations",
            "scheduledOperations.productionOrder",
            "scheduledOperations.productionOrder.routing",
            "scheduledOperations.productionOrder.routing.product",
            "scheduledOperations.operation",
            "scheduledOperations.machine"
    })
    Optional<ScheduleRun> findById(Long scheduleRunId);

    @EntityGraph(attributePaths = {
            "scheduledOperations",
            "scheduledOperations.productionOrder",
            "scheduledOperations.productionOrder.routing",
            "scheduledOperations.productionOrder.routing.product",
            "scheduledOperations.operation",
            "scheduledOperations.machine"
    })
    Optional<ScheduleRun> findTopByOrderByCreatedAtDescIdDesc();
}
