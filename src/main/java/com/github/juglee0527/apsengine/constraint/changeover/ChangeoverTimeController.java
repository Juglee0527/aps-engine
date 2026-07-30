package com.github.juglee0527.apsengine.constraint.changeover;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChangeoverTimeController {

    private final ChangeoverTimeService changeoverTimeService;

    public ChangeoverTimeController(
            ChangeoverTimeService changeoverTimeService
    ) {
        this.changeoverTimeService = changeoverTimeService;
    }

    @PostMapping("/api/v1/machines/{machineId}/changeover-times")
    public ResponseEntity<ChangeoverTimeResponse> create(
            @PathVariable long machineId,
            @Valid @RequestBody ChangeoverTimeCreateRequest request
    ) {
        ChangeoverTime changeoverTime = changeoverTimeService.create(
                machineId,
                request.fromProductId(),
                request.toProductId(),
                request.changeoverMinutes()
        );
        ChangeoverTimeResponse response =
                ChangeoverTimeResponse.from(changeoverTime);
        URI location = URI.create(
                "/api/v1/changeover-times/" + response.id()
        );
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/v1/changeover-times/{changeoverTimeId}")
    public ChangeoverTimeResponse getById(
            @PathVariable long changeoverTimeId
    ) {
        return ChangeoverTimeResponse.from(
                changeoverTimeService.getById(changeoverTimeId)
        );
    }

    @GetMapping("/api/v1/machines/{machineId}/changeover-times")
    public List<ChangeoverTimeResponse> getAllByMachine(
            @PathVariable long machineId
    ) {
        List<ChangeoverTime> changeoverTimes =
                changeoverTimeService.getAllByMachine(machineId);
        List<ChangeoverTimeResponse> responses =
                new ArrayList<>(changeoverTimes.size());
        for (ChangeoverTime changeoverTime : changeoverTimes) {
            responses.add(ChangeoverTimeResponse.from(changeoverTime));
        }
        return responses;
    }
}
