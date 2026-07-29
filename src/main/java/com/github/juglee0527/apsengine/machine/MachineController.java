package com.github.juglee0527.apsengine.machine;

import java.net.URI;

import com.github.juglee0527.apsengine.common.web.PageRequestParameters;
import com.github.juglee0527.apsengine.common.web.PageResponse;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @PostMapping("/api/v1/production-lines/{productionLineId}/machines")
    public ResponseEntity<MachineResponse> create(
            @PathVariable long productionLineId,
            @Valid @RequestBody MachineCreateRequest request
    ) {
        Machine machine = machineService.create(
                productionLineId,
                request.code(),
                request.name(),
                request.status()
        );
        MachineResponse response = MachineResponse.from(machine);
        URI location = URI.create("/api/v1/machines/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/v1/machines/{machineId}")
    public MachineResponse getById(@PathVariable long machineId) {
        return MachineResponse.from(machineService.getById(machineId));
    }

    @GetMapping("/api/v1/production-lines/{productionLineId}/machines")
    public PageResponse<MachineResponse> getPage(
            @PathVariable long productionLineId,
            @Valid @ModelAttribute PageRequestParameters request
    ) {
        Page<Machine> machinePage = machineService.getPageByProductionLine(
                productionLineId,
                request.page(),
                request.size()
        );
        return PageResponse.from(machinePage, MachineResponse::from);
    }
}
