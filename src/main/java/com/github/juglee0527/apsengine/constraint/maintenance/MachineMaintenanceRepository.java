package com.github.juglee0527.apsengine.constraint.maintenance;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MachineMaintenanceRepository
        extends JpaRepository<MachineMaintenance, Long> {

    boolean
    existsByMachine_IdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
            Long machineId,
            OffsetDateTime endAt,
            OffsetDateTime startAt
    );

    @EntityGraph(attributePaths = "machine")
    List<MachineMaintenance>
    findAllByMachine_IdAndActiveTrueOrderByStartAtAsc(
            Long machineId
    );

    @EntityGraph(attributePaths = "machine")
    @Query("""
            select maintenance
            from MachineMaintenance maintenance
            where maintenance.id = :maintenanceId
              and maintenance.active = true
            """)
    Optional<MachineMaintenance> findActiveDetailById(
            @Param("maintenanceId") Long maintenanceId
    );

    @EntityGraph(attributePaths = "machine")
    List<MachineMaintenance>
    findAllByMachine_IdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
            Long machineId,
            OffsetDateTime to,
            OffsetDateTime from
    );

    @EntityGraph(attributePaths = "machine")
    List<MachineMaintenance>
    findAllByMachine_IdInAndActiveTrueAndEndAtGreaterThanOrderByStartAtAsc(
            Set<Long> machineIds,
            OffsetDateTime planningStart
    );
}
