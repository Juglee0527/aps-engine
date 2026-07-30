package com.github.juglee0527.apsengine.planningdata;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PlanningDataCsvParser {

    static final List<String> REQUIRED_HEADERS = List.of(
            "type",
            "factoryCode",
            "lineCode",
            "machineCode",
            "productCode",
            "routingCode",
            "orderNumber",
            "name",
            "status",
            "unit",
            "operationSequence",
            "operationCode",
            "operationName",
            "processingTimeMinutes",
            "quantity",
            "releaseAt",
            "dueAt",
            "priority"
    );

    ParsedCsv parse(byte[] bytes, int maxRows) {
        String csv = decodeUtf8(bytes);
        if (!csv.isEmpty() && csv.charAt(0) == '\ufeff') {
            csv = csv.substring(1);
        }
        List<List<String>> records = parseRecords(csv);
        if (records.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV 헤더와 데이터 행이 필요합니다."
            );
        }
        List<String> headers = records.getFirst().stream()
                .map(String::trim)
                .toList();
        validateHeaders(headers);

        List<ParsedCsvRow> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> values = records.get(index);
            if (values.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (rows.size() >= maxRows) {
                throw new IllegalArgumentException(
                        "CSV 데이터 행은 %d건을 초과할 수 없습니다."
                                .formatted(maxRows)
                );
            }
            int rowNumber = index + 1;
            if (values.size() != headers.size()) {
                rows.add(new ParsedCsvRow(
                        rowNumber,
                        Map.of(),
                        values.size()
                ));
                continue;
            }
            Map<String, String> fields = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                fields.put(
                        headers.get(column),
                        values.get(column).trim()
                );
            }
            rows.add(new ParsedCsvRow(
                    rowNumber,
                    fields,
                    values.size()
            ));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV 데이터 행이 하나 이상 필요합니다."
            );
        }
        return new ParsedCsv(headers.size(), List.copyOf(rows));
    }

    private String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(
                    "CSV 파일은 UTF-8 인코딩이어야 합니다.",
                    exception
            );
        }
    }

    private void validateHeaders(List<String> headers) {
        Set<String> uniqueHeaders = new HashSet<>(headers);
        if (uniqueHeaders.size() != headers.size()) {
            throw new IllegalArgumentException(
                    "CSV 헤더는 중복될 수 없습니다."
            );
        }
        List<String> missing = REQUIRED_HEADERS.stream()
                .filter(header -> !uniqueHeaders.contains(header))
                .toList();
        List<String> unknown = headers.stream()
                .filter(header -> !REQUIRED_HEADERS.contains(header))
                .toList();
        if (!missing.isEmpty() || !unknown.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV 헤더가 올바르지 않습니다. 누락=%s, 알 수 없음=%s"
                            .formatted(missing, unknown)
            );
        }
    }

    private List<List<String>> parseRecords(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;

        for (int index = 0; index < csv.length(); index++) {
            char character = csv.charAt(index);
            if (quoted) {
                if (character == '"') {
                    if (index + 1 < csv.length()
                            && csv.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else {
                    field.append(character);
                }
                continue;
            }
            if (closedQuote) {
                if (character == ',') {
                    addField(record, field);
                    closedQuote = false;
                } else if (character == '\r' || character == '\n') {
                    addField(record, field);
                    addRecord(records, record);
                    record = new ArrayList<>();
                    closedQuote = false;
                    if (character == '\r'
                            && index + 1 < csv.length()
                            && csv.charAt(index + 1) == '\n') {
                        index++;
                    }
                } else if (!Character.isWhitespace(character)) {
                    throw new IllegalArgumentException(
                            "닫힌 따옴표 뒤에는 구분자만 올 수 있습니다."
                    );
                }
                continue;
            }
            if (character == '"') {
                if (!field.isEmpty()) {
                    throw new IllegalArgumentException(
                            "따옴표는 필드 시작 위치에만 사용할 수 있습니다."
                    );
                }
                quoted = true;
            } else if (character == ',') {
                addField(record, field);
            } else if (character == '\r' || character == '\n') {
                addField(record, field);
                addRecord(records, record);
                record = new ArrayList<>();
                if (character == '\r'
                        && index + 1 < csv.length()
                        && csv.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                field.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException(
                    "닫히지 않은 따옴표가 있습니다."
            );
        }
        if (!record.isEmpty() || !field.isEmpty() || closedQuote) {
            addField(record, field);
            addRecord(records, record);
        }
        return records;
    }

    private void addField(
            List<String> record,
            StringBuilder field
    ) {
        record.add(field.toString());
        field.setLength(0);
    }

    private void addRecord(
            List<List<String>> records,
            List<String> record
    ) {
        records.add(List.copyOf(record));
    }

    record ParsedCsv(
            int expectedColumnCount,
            List<ParsedCsvRow> rows
    ) {
    }

    record ParsedCsvRow(
            int rowNumber,
            Map<String, String> fields,
            int actualColumnCount
    ) {
    }
}
