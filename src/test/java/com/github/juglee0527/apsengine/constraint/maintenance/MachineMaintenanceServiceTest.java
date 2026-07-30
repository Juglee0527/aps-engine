package com.github.juglee0527.apsengine.constraint.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
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
class MachineMaintenanceServiceTest {

    private static final OffsetDateTime START =
            OffsetDateTime.parse("2026-08-03T10:00:00+09:00");
    private static final OffsetDateTime END = START.plusHours(1);

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MachineMaintenanceRepository maintenanceRepository;

    @InjectMocks
    private MachineMaintenanceService maintenanceService;

    @Test
    void createsMaintenanceWhenBoundariesDoNotOverlap() {
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine()));
        when(maintenanceRepository
                .existsByMachine_IdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
                        1L,
                        END,
                        START
                )).thenReturn(false);
        when(maintenanceRepository.saveAndFlush(
                any(MachineMaintenance.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        MachineMaintenance result = maintenanceService.create(
                1L,
                START,
                END,
                "정기 점검"
        );

        assertThat(result.startAt()).isEqualTo(START);
        assertThat(result.endAt()).isEqualTo(END);
        verify(maintenanceRepository)
                .existsByMachine_IdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
                        1L,
                        END,
                        START
                );
    }

    @Test
    void rejectsOverlappingMaintenance() {
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine()));
        when(maintenanceRepository
                .existsByMachine_IdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
                        1L,
                        END,
                        START
                )).thenReturn(true);

        assertThatThrownBy(() -> maintenanceService.create(
                1L,
                START,
                END,
                "정기 점검"
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode())
                        .isEqualTo(ErrorCode.MAINTENANCE_OVERLAP)
        );

        verify(maintenanceRepository, never()).saveAndFlush(any());
    }

    @Test
    void convertsInvalidPeriodToInvalidRequest() {
        when(machineRepository.findById(1L))
                .thenReturn(Optional.of(machine()));

        assertThatThrownBy(() -> maintenanceService.create(
                1L,
                START,
                START,
                "정기 점검"
        )).isInstanceOfSatisfying(
                ApplicationException.class,
                exception -> assertThat(exception.errorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST)
        );
    }

    private Machine machine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, "MACHINE-01", "가공 설비");
    }
}
