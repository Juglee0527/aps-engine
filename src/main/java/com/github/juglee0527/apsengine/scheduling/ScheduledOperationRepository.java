package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduledOperationRepository
        extends JpaRepository<ScheduledOperation, Long> {

    @EntityGraph(attributePaths = {
            "productionOrder", "productionOrder.routing",
            "productionOrder.routing.product", "operation", "machine"
    })
    @Query("""
            SELECT task
            FROM ScheduledOperation task
            WHERE task.scheduleRun.id = :scheduleRunId
              AND (:machineId IS NULL OR task.machine.id = :machineId)
              AND (:fromAt IS NULL OR task.endAt > :fromAt)
              AND (:toAt IS NULL OR task.startAt < :toAt)
              AND (:query = ''
                   OR LOWER(task.productionOrder.orderNumber) LIKE CONCAT('%', :query, '%')
                   OR LOWER(task.operation.code) LIKE CONCAT('%', :query, '%'))
            ORDER BY task.startAt ASC, task.id ASC
            """)
    Page<ScheduledOperation> search(
            @Param("scheduleRunId") long scheduleRunId,
            @Param("machineId") Long machineId,
            @Param("fromAt") OffsetDateTime fromAt,
            @Param("toAt") OffsetDateTime toAt,
            @Param("query") String query,
            Pageable pageable
    );

    long countByScheduleRun_Id(Long scheduleRunId);

    @Query("""
            SELECT COUNT(DISTINCT task.productionOrder.id)
            FROM ScheduledOperation task
            WHERE task.scheduleRun.id = :scheduleRunId
            """)
    long countOrders(@Param("scheduleRunId") long scheduleRunId);
}
