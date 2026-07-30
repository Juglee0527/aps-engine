package com.github.juglee0527.apsengine.planningdata;

import java.util.Map;
import java.util.stream.Collectors;

final class PlanningDataCsvTestSupport {

    private PlanningDataCsvTestSupport() {
    }

    static String header() {
        return String.join(
                ",",
                PlanningDataCsvParser.REQUIRED_HEADERS
        );
    }

    static String row(Map<String, String> fields) {
        return PlanningDataCsvParser.REQUIRED_HEADERS.stream()
                .map(header -> escape(fields.getOrDefault(header, "")))
                .collect(Collectors.joining(","));
    }

    private static String escape(String value) {
        if (value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
