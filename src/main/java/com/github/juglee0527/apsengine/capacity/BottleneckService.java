package com.github.juglee0527.apsengine.capacity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenance;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.scheduling.ScheduleRun;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduledOperation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BottleneckService {

    private final ScheduleRunRepository scheduleRunRepository;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final MachineMaintenanceRepository maintenanceRepository;
    private final WorkingTimeCalculator workingTimeCalculator;
    private final BottleneckDetector bottleneckDetector;

    public BottleneckService(
            ScheduleRunRepository scheduleRunRepository,
            WorkingCalendarRepository workingCalendarRepository,
            MachineMaintenanceRepository maintenanceRepository
    ) {
        this.scheduleRunRepository = scheduleRunRepository;
        this.workingCalendarRepository = workingCalendarRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.workingTimeCalculator = new WorkingTimeCalculator();
        this.bottleneckDetector = new BottleneckDetector();
    }

    @Transactional(readOnly = true)
    public BottleneckAnalysis detect(long scheduleRunId) {
        ScheduleRun scheduleRun = scheduleRunRepository
                .findById(scheduleRunId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.SCHEDULE_RUN_NOT_FOUND
                ));
        OffsetDateTime from = applyPlanningOffset(
                scheduleRun.planningStart(),
                scheduleRun.planningOffsetSeconds()
        );
        OffsetDateTime to = applyPlanningOffset(
                scheduleRun.schedulingEnd(),
                scheduleRun.planningOffsetSeconds()
        );

        Map<Long, MachineLoad> loads = collectLoads(scheduleRun);
        if (loads.isEmpty()) {
            return bottleneckDetector.detect(
                    scheduleRunId,
                    from,
                    to,
                    List.of()
            );
        }

        Set<Long> machineIds = Set.copyOf(loads.keySet());
        Map<Long, List<WeeklyWorkingTime>> weeklyTimesByMachine =
                collectWeeklyTimes(machineIds);
        Map<Long, List<UnavailableInterval>> unavailableByMachine =
                collectUnavailableIntervals(machineIds, from);

        List<MachineCapacityInput> inputs =
                new ArrayList<>(loads.size());
        for (MachineLoad load : loads.values()) {
            long machineId = load.machine().id();
            long availableMinutes =
                    workingTimeCalculator.availableMinutes(
                            weeklyTimesByMachine.getOrDefault(
                                    machineId,
                                    List.of()
                            ),
                            unavailableByMachine.getOrDefault(
                                    machineId,
                                    List.of()
                            ),
                            from,
                            to
                    );
            inputs.add(new MachineCapacityInput(
                    machineId,
                    load.machine().code(),
                    load.machine().name(),
                    availableMinutes,
                    load.loadMinutes()
            ));
        }
        return bottleneckDetector.detect(
                scheduleRunId,
                from,
                to,
                inputs
        );
    }

    private Map<Long, MachineLoad> collectLoads(
            ScheduleRun scheduleRun
    ) {
        Map<Long, MachineLoad> loads = new LinkedHashMap<>();
        for (ScheduledOperation operation
                : scheduleRun.scheduledOperations()) {
            Machine machine = operation.machine();
            long operationLoad = Math.addExact(
                    operation.workingMinutes(),
                    operation.changeoverMinutes()
            );
            loads.computeIfAbsent(
                    machine.id(),
                    ignored -> new MachineLoad(machine)
            ).add(operationLoad);
        }
        return loads;
    }

    private Map<Long, List<WeeklyWorkingTime>> collectWeeklyTimes(
            Set<Long> machineIds
    ) {
        Map<Long, List<WeeklyWorkingTime>> result = new HashMap<>();
        for (WorkingCalendar calendar : workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(machineIds)) {
            result.computeIfAbsent(
                    calendar.machine().id(),
                    ignored -> new ArrayList<>()
            ).add(calendar.toWeeklyWorkingTime());
        }
        return result;
    }

    private Map<Long, List<UnavailableInterval>>
    collectUnavailableIntervals(
            Set<Long> machineIds,
            OffsetDateTime planningStart
    ) {
        Map<Long, List<UnavailableInterval>> result =
                new HashMap<>();
        for (MachineMaintenance maintenance : maintenanceRepository
                .findAllByMachine_IdInAndActiveTrueAndEndAtGreaterThanOrderByStartAtAsc(
                        machineIds,
                        planningStart
                )) {
            result.computeIfAbsent(
                    maintenance.machine().id(),
                    ignored -> new ArrayList<>()
            ).add(maintenance.toUnavailableInterval());
        }
        return result;
    }

    private OffsetDateTime applyPlanningOffset(
            OffsetDateTime dateTime,
            int planningOffsetSeconds
    ) {
        return dateTime.withOffsetSameInstant(
                ZoneOffset.ofTotalSeconds(planningOffsetSeconds)
        );
    }

    private static final class MachineLoad {

        private final Machine machine;
        private long loadMinutes;

        private MachineLoad(Machine machine) {
            this.machine = machine;
        }

        private void add(long minutes) {
            loadMinutes = Math.addExact(loadMinutes, minutes);
        }

        private Machine machine() {
            return machine;
        }

        private long loadMinutes() {
            return loadMinutes;
        }
    }
}
