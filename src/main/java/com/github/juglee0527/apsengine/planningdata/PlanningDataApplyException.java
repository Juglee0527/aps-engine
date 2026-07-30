package com.github.juglee0527.apsengine.planningdata;

final class PlanningDataApplyException extends RuntimeException {

    private final int rowNumber;
    private final PlanningDataType dataType;

    PlanningDataApplyException(
            int rowNumber,
            PlanningDataType dataType,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.rowNumber = rowNumber;
        this.dataType = dataType;
    }

    int rowNumber() {
        return rowNumber;
    }

    PlanningDataType dataType() {
        return dataType;
    }
}
