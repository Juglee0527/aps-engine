package com.github.juglee0527.apsengine.planningdata;

import java.util.Locale;

public enum PlanningDataType {
    FACTORY(1),
    PRODUCTION_LINE(2),
    MACHINE(3),
    PRODUCT(3),
    ROUTING(4),
    PRODUCTION_ORDER(5);

    private final int importOrder;

    PlanningDataType(int importOrder) {
        this.importOrder = importOrder;
    }

    int importOrder() {
        return importOrder;
    }

    static PlanningDataType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("type은 필수입니다.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "지원하지 않는 type입니다: " + value.trim(),
                    exception
            );
        }
    }
}
