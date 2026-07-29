package com.github.juglee0527.apsengine.capacity;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.machine.MachineStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkingCalendarService {

    private static final long MAX_AVAILABILITY_RANGE_DAYS = 366;

    private final MachineRepository machineRepository;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final WorkingTimeCalculator workingTimeCalculator;

    public WorkingCalendarService(
            MachineRepository machineRepository,
            WorkingCalendarRepository workingCalendarRepository
    ) {
        this.machineRepository = machineRepository;
        this.workingCalendarRepository = workingCalendarRepository;
        this.workingTimeCalculator = new WorkingTimeCalculator();
    }

    @Transactional
    public List<WorkingCalendar> create(
            long machineId,
            List<WorkingCalendarEntryRequest> entries
    ) {
        Machine machine = getUsableMachine(machineId);
        if (entries == null || entries.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "근무시간은 하나 이상 필요합니다."
            );
        }

        List<WorkingCalendar> existing = workingCalendarRepository
                .findAllByMachine_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                        machineId
                );
        List<WorkingCalendar> additions = new ArrayList<>(entries.size());
        for (WorkingCalendarEntryRequest entry : entries) {
            WorkingCalendar calendar;
            try {
                calendar = WorkingCalendar.create(
                        machine,
                        entry.dayOfWeek(),
                        entry.startTime(),
                        entry.endTime()
                );
            } catch (IllegalArgumentException exception) {
                throw new ApplicationException(
                        ErrorCode.INVALID_REQUEST,
                        exception.getMessage(),
                        exception
                );
            }
            validateNoOverlap(existing, additions, calendar);
            additions.add(calendar);
        }
        return workingCalendarRepository.saveAllAndFlush(additions);
    }

    @Transactional(readOnly = true)
    public List<WorkingCalendar> getAllByMachine(long machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ApplicationException(ErrorCode.MACHINE_NOT_FOUND);
        }
        List<WorkingCalendar> calendars = new ArrayList<>(
                workingCalendarRepository
                .findAllByMachine_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                        machineId
                )
        );
        calendars.sort(
                Comparator.comparingInt(
                                (WorkingCalendar calendar) ->
                                        calendar.dayOfWeek().getValue()
                        )
                        .thenComparing(WorkingCalendar::startTime)
        );
        return List.copyOf(calendars);
    }

    @Transactional(readOnly = true)
    public MachineAvailabilityResponse getAvailability(
            long machineId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (from == null
                || to == null
                || !to.isAfter(from)
                || Duration.between(from, to).toDays()
                > MAX_AVAILABILITY_RANGE_DAYS) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "가용시간 조회 구간은 366일 이내의 올바른 범위여야 합니다."
            );
        }
        List<WeeklyWorkingTime> weeklyTimes =
                getWeeklyTimes(machineId);
        List<AvailabilityInterval> intervals =
                workingTimeCalculator.intervalsBetween(
                        weeklyTimes,
                        from,
                        to
                );
        long availableMinutes = 0;
        for (AvailabilityInterval interval : intervals) {
            availableMinutes = Math.addExact(
                    availableMinutes,
                    Duration.between(
                            interval.startAt(),
                            interval.endAt()
                    ).toMinutes()
            );
        }
        return new MachineAvailabilityResponse(
                machineId,
                from,
                to,
                availableMinutes,
                intervals
        );
    }

    @Transactional(readOnly = true)
    public List<WeeklyWorkingTime> getWeeklyTimes(long machineId) {
        List<WorkingCalendar> calendars = getAllByMachine(machineId);
        List<WeeklyWorkingTime> weeklyTimes =
                new ArrayList<>(calendars.size());
        for (WorkingCalendar calendar : calendars) {
            weeklyTimes.add(calendar.toWeeklyWorkingTime());
        }
        return List.copyOf(weeklyTimes);
    }

    private Machine getUsableMachine(long machineId) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.MACHINE_NOT_FOUND));
        if (machine.status() == MachineStatus.INACTIVE) {
            throw new ApplicationException(ErrorCode.MACHINE_INACTIVE);
        }
        return machine;
    }

    private void validateNoOverlap(
            List<WorkingCalendar> existing,
            List<WorkingCalendar> additions,
            WorkingCalendar candidate
    ) {
        for (WorkingCalendar calendar : existing) {
            if (overlaps(calendar, candidate)) {
                throw overlapException();
            }
        }
        for (WorkingCalendar calendar : additions) {
            if (overlaps(calendar, candidate)) {
                throw overlapException();
            }
        }
    }

    private boolean overlaps(
            WorkingCalendar left,
            WorkingCalendar right
    ) {
        return left.dayOfWeek() == right.dayOfWeek()
                && left.startTime().isBefore(right.endTime())
                && right.startTime().isBefore(left.endTime());
    }

    private ApplicationException overlapException() {
        return new ApplicationException(
                ErrorCode.WORKING_CALENDAR_OVERLAP
        );
    }
}
