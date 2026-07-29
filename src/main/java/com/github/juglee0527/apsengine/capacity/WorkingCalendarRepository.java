package com.github.juglee0527.apsengine.capacity;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkingCalendarRepository
        extends JpaRepository<WorkingCalendar, Long> {

    @EntityGraph(attributePaths = "machine")
    List<WorkingCalendar>
    findAllByMachine_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
            Long machineId
    );

    @EntityGraph(attributePaths = "machine")
    List<WorkingCalendar> findAllByMachine_IdInAndActiveTrue(
            Collection<Long> machineIds
    );
}
