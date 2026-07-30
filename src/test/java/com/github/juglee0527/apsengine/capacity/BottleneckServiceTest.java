package com.github.juglee0527.apsengine.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduleRun;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunRepository;
import com.github.juglee0527.apsengine.scheduling.SchedulingPlan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BottleneckServiceTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    @Mock
    private ScheduleRunRepository scheduleRunRepository;

    @Mock
    private WorkingCalendarRepository workingCalendarRepository;

    @Mock
    private MachineMaintenanceRepository maintenanceRepository;

    private BottleneckService bottleneckService;

    @BeforeEach
    void setUp() {
        bottleneckService = new BottleneckService(
                scheduleRunRepository,
                workingCalendarRepository,
                maintenanceRepository
        );
    }

    @Test
    void returnsEmptyResultWithoutQueryingCapacityForEmptyRun() {
        ScheduleRun emptyRun = ScheduleRun.create(
                UUID.randomUUID(),
                new SchedulingPlan(
                        PLANNING_START,
                        PLANNING_START,
                        List.of()
                ),
                PLANNING_START
        );
        when(scheduleRunRepository.findById(1L))
                .thenReturn(Optional.of(emptyRun));

        BottleneckAnalysis result = bottleneckService.detect(1L);

        assertThat(result.candidates()).isEmpty();
        assertThat(result.from()).isEqualTo(PLANNING_START);
        assertThat(result.to()).isEqualTo(PLANNING_START);
        verifyNoInteractions(
                workingCalendarRepository,
                maintenanceRepository
        );
    }

    @Test
    void rejectsMissingScheduleRun() {
        when(scheduleRunRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bottleneckService.detect(1L))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.SCHEDULE_RUN_NOT_FOUND)
                );
    }
}
