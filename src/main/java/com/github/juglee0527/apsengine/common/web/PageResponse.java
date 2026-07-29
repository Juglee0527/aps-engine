package com.github.juglee0527.apsengine.common.web;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <S, T> PageResponse<T> from(
            Page<S> sourcePage,
            Function<S, T> mapper
    ) {
        List<T> content = new ArrayList<>(sourcePage.getNumberOfElements());

        for (S source : sourcePage.getContent()) {
            content.add(mapper.apply(source));
        }

        return new PageResponse<>(
                content,
                sourcePage.getNumber(),
                sourcePage.getSize(),
                sourcePage.getTotalElements(),
                sourcePage.getTotalPages(),
                sourcePage.isFirst(),
                sourcePage.isLast()
        );
    }
}
