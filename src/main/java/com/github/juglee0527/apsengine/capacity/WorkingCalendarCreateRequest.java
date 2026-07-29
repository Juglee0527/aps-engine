package com.github.juglee0527.apsengine.capacity;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record WorkingCalendarCreateRequest(
        @NotEmpty(message = "근무시간은 하나 이상 필요합니다.")
        List<@Valid WorkingCalendarEntryRequest> entries
) {

    public WorkingCalendarCreateRequest {
        entries = entries == null ? null : List.copyOf(entries);
    }
}
