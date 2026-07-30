package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlannedLeadTimeServiceTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.parse("2026-08-03T08:00:00+09:00");

    @Mock
    private ScheduleRunRepository scheduleRunRepository;

    private PlannedLeadTimeService plannedLeadTimeService;

    @BeforeEach
    void setUp() {
        plannedLeadTimeService =
                new PlannedLeadTimeService(scheduleRunRepository);
    }

    @Test
    void returnsEmptyResultForRunWithoutOperations() {
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

        assertThat(plannedLeadTimeService.calculate(1L)).isEmpty();
    }

    @Test
    void rejectsMissingScheduleRun() {
        when(scheduleRunRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                plannedLeadTimeService.calculate(1L))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.SCHEDULE_RUN_NOT_FOUND)
                );
    }
}
