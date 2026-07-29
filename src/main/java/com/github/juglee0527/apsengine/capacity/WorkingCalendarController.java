package com.github.juglee0527.apsengine.capacity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkingCalendarController {

    private final WorkingCalendarService workingCalendarService;

    public WorkingCalendarController(
            WorkingCalendarService workingCalendarService
    ) {
        this.workingCalendarService = workingCalendarService;
    }

    @PostMapping("/api/v1/machines/{machineId}/working-calendars")
    public List<WorkingCalendarResponse> create(
            @PathVariable long machineId,
            @Valid @RequestBody WorkingCalendarCreateRequest request
    ) {
        return toResponses(workingCalendarService.create(
                machineId,
                request.entries()
        ));
    }

    @GetMapping("/api/v1/machines/{machineId}/working-calendars")
    public List<WorkingCalendarResponse> getAllByMachine(
            @PathVariable long machineId
    ) {
        return toResponses(
                workingCalendarService.getAllByMachine(machineId)
        );
    }

    @GetMapping("/api/v1/machines/{machineId}/availability")
    public MachineAvailabilityResponse getAvailability(
            @PathVariable long machineId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        return workingCalendarService.getAvailability(machineId, from, to);
    }

    private List<WorkingCalendarResponse> toResponses(
            List<WorkingCalendar> calendars
    ) {
        List<WorkingCalendarResponse> responses =
                new ArrayList<>(calendars.size());
        for (WorkingCalendar calendar : calendars) {
            responses.add(WorkingCalendarResponse.from(calendar));
        }
        return responses;
    }
}
