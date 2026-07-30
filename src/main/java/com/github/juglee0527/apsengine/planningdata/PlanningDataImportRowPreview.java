package com.github.juglee0527.apsengine.planningdata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PlanningDataImportRowPreview(
        int rowNumber,
        String type,
        boolean valid,
        Map<String, String> normalizedValues,
        List<PlanningDataImportRowError> errors
) {

    public PlanningDataImportRowPreview {
        normalizedValues = Collections.unmodifiableMap(
                new LinkedHashMap<>(normalizedValues)
        );
        errors = List.copyOf(errors);
    }
}
