package com.github.juglee0527.apsengine.machine;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

public record MachinePageResponse(
        List<MachineResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public MachinePageResponse {
        content = List.copyOf(content);
    }

    public static MachinePageResponse from(Page<Machine> machinePage) {
        List<MachineResponse> content =
                new ArrayList<>(machinePage.getNumberOfElements());

        for (Machine machine : machinePage.getContent()) {
            content.add(MachineResponse.from(machine));
        }

        return new MachinePageResponse(
                content,
                machinePage.getNumber(),
                machinePage.getSize(),
                machinePage.getTotalElements(),
                machinePage.getTotalPages(),
                machinePage.isFirst(),
                machinePage.isLast()
        );
    }
}

