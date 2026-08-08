package com.github.juglee0527.apsengine.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT run
            FROM ScheduleRun run
            ORDER BY run.createdAt DESC, run.id DESC
            """)
    List<ScheduleRun> findLatestSummary(Pageable pageable);

    @Query("SELECT run FROM ScheduleRun run WHERE run.id = :scheduleRunId")
    Optional<ScheduleRun> findSummaryById(
            @Param("scheduleRunId") Long scheduleRunId
    );
}
