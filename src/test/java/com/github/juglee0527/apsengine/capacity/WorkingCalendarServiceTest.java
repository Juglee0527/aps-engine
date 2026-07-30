package com.github.juglee0527.apsengine.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenance;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkingCalendarServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private WorkingCalendarRepository workingCalendarRepository;

    @Mock
    private MachineMaintenanceRepository maintenanceRepository;

    @InjectMocks
    private WorkingCalendarService workingCalendarService;

    @Test
    void createsWorkingCalendarEntries() {
        Machine machine = machine();
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine));
        when(workingCalendarRepository
                .findAllByMachine_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                        1L
                )).thenReturn(List.of());
        when(workingCalendarRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<WorkingCalendar> calendars = workingCalendarService.create(
                1L,
                List.of(entry(DayOfWeek.MONDAY, 8, 17))
        );

        assertThat(calendars).hasSize(1);
        assertThat(calendars.getFirst().dayOfWeek())
                .isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void rejectsOverlappingTime() {
        Machine machine = machine();
        WorkingCalendar existing = WorkingCalendar.create(
                machine,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0)
        );
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine));
        when(workingCalendarRepository
                .findAllByMachine_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                        1L
                )).thenReturn(List.of(existing));

        assertThatThrownBy(() -> workingCalendarService.create(
                1L,
                List.of(entry(DayOfWeek.MONDAY, 11, 17))
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode())
                        .isEqualTo(ErrorCode.WORKING_CALENDAR_OVERLAP)
        );
    }

    @Test
    void excludesMaintenanceFromAvailability() {
        Machine machine = machine();
        WorkingCalendar calendar = WorkingCalendar.create(
                machine,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        OffsetDateTime to =
                OffsetDateTime.parse("2026-08-03T17:00:00+09:00");
        MachineMaintenance maintenance = MachineMaintenance.create(
                machine,
                OffsetDateTime.parse("2026-08-03T10:00:00+09:00"),
                OffsetDateTime.parse("2026-08-03T11:00:00+09:00"),
                "정기 점검"
        );
        when(machineRepository.existsById(1L)).thenReturn(true);
        when(workingCalendarRepository
                .findAllByMachine_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                        1L
                )).thenReturn(List.of(calendar));
        when(maintenanceRepository
                .findAllByMachine_IdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
                        1L,
                        to,
                        from
                )).thenReturn(List.of(maintenance));

        MachineAvailabilityResponse result =
                workingCalendarService.getAvailability(1L, from, to);

        assertThat(result.availableMinutes()).isEqualTo(480);
        assertThat(result.intervals()).hasSize(2);
    }

    private WorkingCalendarEntryRequest entry(
            DayOfWeek dayOfWeek,
            int startHour,
            int endHour
    ) {
        return new WorkingCalendarEntryRequest(
                dayOfWeek,
                LocalTime.of(startHour, 0),
                LocalTime.of(endHour, 0)
        );
    }

    private Machine machine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, "MACHINE-01", "가공 설비");
    }
}
