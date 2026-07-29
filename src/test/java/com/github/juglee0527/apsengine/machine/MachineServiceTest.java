package com.github.juglee0527.apsengine.machine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock
    private ProductionLineRepository productionLineRepository;

    @Mock
    private MachineRepository machineRepository;

    @InjectMocks
    private MachineService machineService;

    @Test
    void createsMachineInProductionLine() {
        ProductionLine productionLine = productionLine();
        when(productionLineRepository.findById(1L))
                .thenReturn(Optional.of(productionLine));
        when(machineRepository.existsByProductionLine_IdAndCode(
                1L,
                "MACHINE-01"
        )).thenReturn(false);
        when(machineRepository.saveAndFlush(any(Machine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Machine machine =
                machineService.create(
                        1L,
                        "machine-01",
                        "절단 설비",
                        MachineStatus.AVAILABLE
                );

        assertThat(machine.productionLine()).isSameAs(productionLine);
        assertThat(machine.code()).isEqualTo("MACHINE-01");
    }

    @Test
    void rejectsMissingProductionLine() {
        when(productionLineRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertErrorCode(
                () -> machineService.create(
                        999L,
                        "MACHINE-01",
                        "절단 설비",
                        MachineStatus.AVAILABLE
                ),
                ErrorCode.PRODUCTION_LINE_NOT_FOUND
        );

        verify(machineRepository, never()).saveAndFlush(any(Machine.class));
    }

    @Test
    void rejectsDuplicatedMachineCode() {
        ProductionLine productionLine = productionLine();
        when(productionLineRepository.findById(1L))
                .thenReturn(Optional.of(productionLine));
        when(machineRepository.existsByProductionLine_IdAndCode(
                1L,
                "MACHINE-01"
        )).thenReturn(true);

        assertErrorCode(
                () -> machineService.create(
                        1L,
                        "machine-01",
                        "절단 설비",
                        MachineStatus.AVAILABLE
                ),
                ErrorCode.MACHINE_CODE_DUPLICATED
        );
    }

    @Test
    void convertsUniqueConstraintRaceToDuplicatedCodeError() {
        ProductionLine productionLine = productionLine();
        when(productionLineRepository.findById(1L))
                .thenReturn(Optional.of(productionLine));
        when(machineRepository.existsByProductionLine_IdAndCode(
                1L,
                "MACHINE-01"
        )).thenReturn(false);
        when(machineRepository.saveAndFlush(any(Machine.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertErrorCode(
                () -> machineService.create(
                        1L,
                        "MACHINE-01",
                        "절단 설비",
                        MachineStatus.AVAILABLE
                ),
                ErrorCode.MACHINE_CODE_DUPLICATED
        );
    }

    @Test
    void rejectsInvalidProductionLineId() {
        assertErrorCode(
                () -> machineService.create(
                        0L,
                        "MACHINE-01",
                        "절단 설비",
                        MachineStatus.AVAILABLE
                ),
                ErrorCode.INVALID_REQUEST
        );

        verify(productionLineRepository, never()).findById(any());
    }

    @Test
    void getsMachineById() {
        Machine machine =
                Machine.create(productionLine(), "MACHINE-01", "절단 설비");
        when(machineRepository.findById(1L)).thenReturn(Optional.of(machine));

        assertThat(machineService.getById(1L)).isSameAs(machine);
    }

    @Test
    void throwsNotFoundForMissingMachine() {
        when(machineRepository.findById(999L)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> machineService.getById(999L),
                ErrorCode.MACHINE_NOT_FOUND
        );
    }

    @Test
    void getsMachinePageByProductionLine() {
        Machine machine =
                Machine.create(productionLine(), "MACHINE-01", "절단 설비");
        PageRequest pageRequest = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );
        Page<Machine> expectedPage =
                new PageImpl<>(List.of(machine), pageRequest, 1);
        when(productionLineRepository.existsById(1L)).thenReturn(true);
        when(machineRepository.findAllByProductionLine_Id(1L, pageRequest))
                .thenReturn(expectedPage);

        Page<Machine> result =
                machineService.getPageByProductionLine(1L, 0, 20);

        assertThat(result).isSameAs(expectedPage);
    }

    private ProductionLine productionLine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        return ProductionLine.create(factory, "LINE-01", "조립 라인");
    }

    private void assertErrorCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ErrorCode errorCode
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(errorCode)
                );
    }
}
