package com.github.juglee0527.apsengine.factory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

public record FactoryPageResponse(
        List<FactoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public FactoryPageResponse {
        content = List.copyOf(content);
    }

    public static FactoryPageResponse from(Page<Factory> factoryPage) {
        List<FactoryResponse> content =
                new ArrayList<>(factoryPage.getNumberOfElements());

        for (Factory factory : factoryPage.getContent()) {
            content.add(FactoryResponse.from(factory));
        }

        return new FactoryPageResponse(
                content,
                factoryPage.getNumber(),
                factoryPage.getSize(),
                factoryPage.getTotalElements(),
                factoryPage.getTotalPages(),
                factoryPage.isFirst(),
                factoryPage.isLast()
        );
    }
}

