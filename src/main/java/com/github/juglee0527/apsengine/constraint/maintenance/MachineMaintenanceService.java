package com.github.juglee0527.apsengine.constraint.maintenance;

import java.time.OffsetDateTime;
import java.util.List;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.machine.MachineStatus;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineMaintenanceService {

    private static final String OVERLAP_CONSTRAINT =
            "ex_machine_maintenance_no_overlap";

    private final MachineRepository machineRepository;
    private final MachineMaintenanceRepository maintenanceRepository;

    public MachineMaintenanceService(
            MachineRepository machineRepository,
            MachineMaintenanceRepository maintenanceRepository
    ) {
        this.machineRepository = machineRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional
    public MachineMaintenance create(
            long machineId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String reason
    ) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.MACHINE_NOT_FOUND));
        if (machine.status() == MachineStatus.INACTIVE) {
            throw new ApplicationException(ErrorCode.MACHINE_INACTIVE);
        }
        MachineMaintenance maintenance;
        try {
            maintenance = MachineMaintenance.create(
                    machine,
                    startAt,
                    endAt,
                    reason
            );
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
        if (maintenanceRepository
                .existsByMachine_IdAndActiveTrueAndStartAtLessThanAndEndAtGreaterThan(
                        machineId,
                        endAt,
                        startAt
                )) {
            throw overlapException();
        }
        try {
            return maintenanceRepository.saveAndFlush(maintenance);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, OVERLAP_CONSTRAINT)) {
                throw new ApplicationException(
                        ErrorCode.MAINTENANCE_OVERLAP,
                        ErrorCode.MAINTENANCE_OVERLAP.defaultMessage(),
                        exception
                );
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public MachineMaintenance getById(long maintenanceId) {
        return maintenanceRepository
                .findActiveDetailById(maintenanceId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.MAINTENANCE_NOT_FOUND
                ));
    }

    @Transactional(readOnly = true)
    public List<MachineMaintenance> getAllByMachine(long machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ApplicationException(ErrorCode.MACHINE_NOT_FOUND);
        }
        return maintenanceRepository
                .findAllByMachine_IdAndActiveTrueOrderByStartAtAsc(machineId);
    }

    private ApplicationException overlapException() {
        return new ApplicationException(ErrorCode.MAINTENANCE_OVERLAP);
    }

    private boolean hasConstraint(
            Throwable exception,
            String constraintName
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null
                    && cause.getMessage().contains(constraintName)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
