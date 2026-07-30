package com.github.juglee0527.apsengine.planningdata;

public record PlanningDataImportRowError(
        String field,
        String code,
        String message
) {
}
