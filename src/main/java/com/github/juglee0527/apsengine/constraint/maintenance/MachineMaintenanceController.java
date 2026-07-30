package com.github.juglee0527.apsengine.constraint.maintenance;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MachineMaintenanceController {

    private final MachineMaintenanceService maintenanceService;

    public MachineMaintenanceController(
            MachineMaintenanceService maintenanceService
    ) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/api/v1/machines/{machineId}/maintenances")
    public ResponseEntity<MachineMaintenanceResponse> create(
            @PathVariable long machineId,
            @Valid @RequestBody MachineMaintenanceCreateRequest request
    ) {
        MachineMaintenanceResponse response =
                MachineMaintenanceResponse.from(
                        maintenanceService.create(
                                machineId,
                                request.startAt(),
                                request.endAt(),
                                request.reason()
                        )
                );
        return ResponseEntity.created(URI.create(
                "/api/v1/maintenances/" + response.id()
        )).body(response);
    }

    @GetMapping("/api/v1/maintenances/{maintenanceId}")
    public MachineMaintenanceResponse getById(
            @PathVariable long maintenanceId
    ) {
        return MachineMaintenanceResponse.from(
                maintenanceService.getById(maintenanceId)
        );
    }

    @GetMapping("/api/v1/machines/{machineId}/maintenances")
    public List<MachineMaintenanceResponse> getAllByMachine(
            @PathVariable long machineId
    ) {
        return maintenanceService.getAllByMachine(machineId)
                .stream()
                .map(MachineMaintenanceResponse::from)
                .toList();
    }
}
