package com.github.juglee0527.apsengine.planningdata;

final class PlanningDataKeys {

    private static final String SEPARATOR = "\u001f";

    private PlanningDataKeys() {
    }

    static String line(String factoryCode, String lineCode) {
        return factoryCode + SEPARATOR + lineCode;
    }

    static String machine(
            String factoryCode,
            String lineCode,
            String machineCode
    ) {
        return line(factoryCode, lineCode)
                + SEPARATOR
                + machineCode;
    }

    static String routing(String productCode, String routingCode) {
        return productCode + SEPARATOR + routingCode;
    }
}
