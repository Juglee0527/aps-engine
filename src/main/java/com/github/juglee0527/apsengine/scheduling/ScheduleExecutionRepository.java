package com.github.juglee0527.apsengine.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleExecutionRepository
        extends JpaRepository<ScheduleExecution, Long> {

    @EntityGraph(attributePaths = {
            "sourceScheduleRun",
            "resultScheduleRun"
    })
    Optional<ScheduleExecution> findByExecutionKey(UUID executionKey);

    @Override
    @EntityGraph(attributePaths = {
            "sourceScheduleRun",
            "resultScheduleRun"
    })
    Optional<ScheduleExecution> findById(Long scheduleExecutionId);

    List<ScheduleExecution> findAllByStatusOrderByCreatedAtAscIdAsc(
            ScheduleExecutionStatus status
    );

    @EntityGraph(attributePaths = {
            "sourceScheduleRun",
            "resultScheduleRun"
    })
    List<ScheduleExecution> findAllByOrderByCreatedAtDescIdDesc(
            Pageable pageable
    );
}
