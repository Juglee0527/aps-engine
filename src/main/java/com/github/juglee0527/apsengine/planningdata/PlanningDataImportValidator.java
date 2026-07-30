package com.github.juglee0527.apsengine.planningdata;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.github.juglee0527.apsengine.common.domain.BusinessCodeNormalizer;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineStatus;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;

final class PlanningDataImportValidator {

    List<PlanningDataImportRowPreview> validate(
            PlanningDataCsvParser.ParsedCsv csv,
            ExistingPlanningData existing
    ) {
        ValidationState state = new ValidationState(existing);
        List<PlanningDataImportRowPreview> previews = new ArrayList<>();
        int lastImportOrder = 0;
        for (PlanningDataCsvParser.ParsedCsvRow row : csv.rows()) {
            RowValidation validation = new RowValidation(row.rowNumber());
            if (row.fields().isEmpty()) {
                validation.error(
                        "row",
                        "COLUMN_COUNT_MISMATCH",
                        "열 개수는 %d개여야 하지만 %d개입니다."
                                .formatted(
                                        csv.expectedColumnCount(),
                                        row.actualColumnCount()
                                )
                );
                previews.add(validation.preview(null));
                continue;
            }

            PlanningDataType type = parseType(row, validation);
            if (type == null) {
                previews.add(validation.preview(row.fields().get("type")));
                continue;
            }
            if (type.importOrder() < lastImportOrder) {
                validation.error(
                        "type",
                        "REFERENCE_ORDER_INVALID",
                        "행 타입 순서는 FACTORY → PRODUCTION_LINE → "
                                + "MACHINE/PRODUCT → ROUTING → "
                                + "PRODUCTION_ORDER여야 합니다."
                );
            }
            lastImportOrder = Math.max(lastImportOrder, type.importOrder());
            if (validation.hasErrors()) {
                previews.add(validation.preview(type.name()));
                continue;
            }
            validateRow(type, row.fields(), validation, state);
            previews.add(validation.preview(type.name()));
        }
        return List.copyOf(previews);
    }

    private PlanningDataType parseType(
            PlanningDataCsvParser.ParsedCsvRow row,
            RowValidation validation
    ) {
        try {
            return PlanningDataType.parse(row.fields().get("type"));
        } catch (IllegalArgumentException exception) {
            validation.error(
                    "type",
                    "INVALID_TYPE",
                    exception.getMessage()
            );
            return null;
        }
    }

    private void validateRow(
            PlanningDataType type,
            Map<String, String> fields,
            RowValidation validation,
            ValidationState state
    ) {
        switch (type) {
            case FACTORY -> validateFactory(fields, validation, state);
            case PRODUCTION_LINE ->
                    validateLine(fields, validation, state);
            case MACHINE -> validateMachine(fields, validation, state);
            case PRODUCT -> validateProduct(fields, validation, state);
            case ROUTING -> validateRouting(fields, validation, state);
            case PRODUCTION_ORDER ->
                    validateOrder(fields, validation, state);
        }
    }

    private void validateFactory(
            Map<String, String> fields,
            RowValidation validation,
            ValidationState state
    ) {
        String code = normalizeCode(
                fields,
                "factoryCode",
                "공장 코드",
                validation
        );
        String name = required(fields, "name", validation);
        if (code == null || name == null) {
            return;
        }
        if (state.existing().factories().containsKey(code)) {
            validation.duplicate("factoryCode", "기존 공장 코드입니다.");
            return;
        }
        if (state.factories().containsKey(code)) {
            validation.duplicate("factoryCode", "파일 안에서 공장 코드가 중복됩니다.");
            return;
        }
        try {
            Factory factory = Factory.create(code, name);
            state.factories().put(code, factory);
            validation.value("factoryCode", factory.code());
            validation.value("name", factory.name());
        } catch (IllegalArgumentException exception) {
            validation.invalid("row", exception.getMessage());
        }
    }

    private void validateLine(
            Map<String, String> fields,
            RowValidation validation,
            ValidationState state
    ) {
        String factoryCode = normalizeCode(
                fields,
                "factoryCode",
                "공장 코드",
                validation
        );
        String lineCode = normalizeCode(
                fields,
                "lineCode",
                "생산라인 코드",
                validation
        );
        String name = required(fields, "name", validation);
        if (factoryCode == null || lineCode == null || name == null) {
            return;
        }
        Factory factory = state.factory(factoryCode);
        if (factory == null) {
            validation.reference(
                    "factoryCode",
                    "앞선 유효 행이나 DB에서 공장을 찾을 수 없습니다."
            );
            return;
        }
        String key = PlanningDataKeys.line(factoryCode, lineCode);
        if (state.existing().lines().containsKey(key)) {
            validation.duplicate("lineCode", "기존 생산라인 코드입니다.");
            return;
        }
        if (state.lines().containsKey(key)) {
            validation.duplicate("lineCode", "파일 안에서 생산라인 코드가 중복됩니다.");
            return;
        }
        try {
            ProductionLine line =
                    ProductionLine.create(factory, lineCode, name);
            state.lines().put(key, line);
            validation.value("factoryCode", factoryCode);
            validation.value("lineCode", line.code());
            validation.value("name", line.name());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            validation.invalid("row", exception.getMessage());
        }
    }

    private void validateMachine(
            Map<String, String> fields,
            RowValidation validation,
            ValidationState state
    ) {
        String factoryCode = normalizeCode(
                fields, "factoryCode", "공장 코드", validation);
        String lineCode = normalizeCode(
                fields, "lineCode", "생산라인 코드", validation);
        String machineCode = normalizeCode(
                fields, "machineCode", "설비 코드", validation);
        String name = required(fields, "name", validation);
        MachineStatus status = parseEnum(
                fields,
                "status",
                MachineStatus.class,
                validation
        );
        if (factoryCode == null || lineCode == null
                || machineCode == null || name == null || status == null) {
            return;
        }
        ProductionLine line = state.line(factoryCode, lineCode);
        if (line == null) {
            validation.reference(
                    "lineCode",
                    "앞선 유효 행이나 DB에서 생산라인을 찾을 수 없습니다."
            );
            return;
        }
        String key = PlanningDataKeys.machine(
                factoryCode,
                lineCode,
                machineCode
        );
        if (state.existing().machines().containsKey(key)) {
            validation.duplicate("machineCode", "기존 설비 코드입니다.");
            return;
        }
        if (state.machines().containsKey(key)) {
            validation.duplicate("machineCode", "파일 안에서 설비 코드가 중복됩니다.");
            return;
        }
        try {
            Machine machine = Machine.create(
                    line,
                    machineCode,
                    name,
                    status
            );
            state.machines().put(key, machine);
            validation.value("factoryCode", factoryCode);
            validation.value("lineCode", lineCode);
            validation.value("machineCode", machine.code());
            validation.value("name", machine.name());
            validation.value("status", machine.status().name());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            validation.invalid("row", exception.getMessage());
        }
    }

    private void validateProduct(
            Map<String, String> fields,
            RowValidation validation,
            ValidationState state
    ) {
        String productCode = normalizeCode(
                fields, "productCode", "품목 코드", validation);
        String name = required(fields, "name", validation);
        ProductUnit unit = parseEnum(
                fields,
                "unit",
                ProductUnit.class,
                validation
        );
        if (productCode == null || name == null || unit == null) {
            return;
        }
        if (state.existing().products().containsKey(productCode)) {
            validation.duplicate("productCode", "기존 품목 코드입니다.");
            return;
        }
        if (state.products().containsKey(productCode)) {
            validation.duplicate("productCode", "파일 안에서 품목 코드가 중복됩니다.");
            return;
        }
        try {
            Product product = Product.create(productCode, name, unit);
            state.products().put(product.code(), product);
            validation.value("productCode", product.code());
            validation.value("name", product.name());
            validation.value("unit", product.unit().name());
        } catch (IllegalArgumentException exception) {
            validation.invalid("row", exception.getMessage());
        }
    }

    private void validateRouting(
            Map<String, String> fields,
            RowValidation validation,
            ValidationState state
    ) {
        String productCode = normalizeCode(
                fields, "productCode", "품목 코드", validation);
        String routingCode = normalizeCode(
                fields, "routingCode", "Routing 코드", validation);
        String name = required(fields, "name", validation);
        String factoryCode = normalizeCode(
                fields, "factoryCode", "공장 코드", validation);
        String lineCode = normalizeCode(
                fields, "lineCode", "생산라인 코드", validation);
        String machineCode = normalizeCode(
                fields, "machineCode", "설비 코드", validation);
        Integer sequence = parseInt(
                fields, "operationSequence", 1, Integer.MAX_VALUE, validation);
        String operationCode = normalizeCode(
                fields, "operationCode", "Operation 코드", validation);
        String operationName =
                required(fields, "operationName", validation);
        Integer processingTime = parseInt(
                fields,
                "processingTimeMinutes",
                1,
                10_080,
                validation
        );
        if (validation.hasErrors()) {
            return;
        }

        Product product = state.product(productCode);
        if (product == null) {
            validation.reference(
                    "productCode",
                    "앞선 유효 행이나 DB에서 품목을 찾을 수 없습니다."
            );
        }
        Machine machine = state.machine(
                factoryCode,
                lineCode,
                machineCode
        );
        if (machine == null) {
            validation.reference(
                    "machineCode",
                    "앞선 유효 행이나 DB에서 설비를 찾을 수 없습니다."
            );
        } else if (machine.status() == MachineStatus.INACTIVE) {
            validation.invalid(
                    "machineCode",
                    "비활성 설비는 Operation에 배정할 수 없습니다."
            );
        }
        if (validation.hasErrors()) {
            return;
        }

        String key = PlanningDataKeys.routing(productCode, routingCode);
        if (state.existing().routings().containsKey(key)) {
            validation.duplicate("routingCode", "기존 Routing 코드입니다.");
            return;
        }
        Routing routing = state.routings().get(key);
        boolean newRouting = routing == null;
        try {
            if (newRouting) {
                routing = Routing.create(product, routingCode, name);
            } else if (!routing.name().equals(name.trim())) {
                validation.invalid(
                        "name",
                        "같은 Routing의 이름은 모든 행에서 같아야 합니다."
                );
                return;
            }
            routing.addOperation(
                    sequence,
                    operationCode,
                    operationName,
                    processingTime,
                    machine
            );
            if (newRouting) {
                state.routings().put(key, routing);
            }
            validation.value("productCode", productCode);
            validation.value("routingCode", routing.code());
            validation.value("name", routing.name());
            validation.value("factoryCode", factoryCode);
            validation.value("lineCode", lineCode);
            validation.value("machineCode", machineCode);
            validation.value("operationSequence", sequence.toString());
            validation.value("operationCode", operationCode);
            validation.value("operationName", operationName.trim());
            validation.value(
                    "processingTimeMinutes",
                    processingTime.toString()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            validation.invalid("row", exception.getMessage());
        }
    }

    private void validateOrder(
            Map<String, String> fields,
            RowValidation validation,
            ValidationState state
    ) {
        String productCode = normalizeCode(
                fields, "productCode", "품목 코드", validation);
        String routingCode = normalizeCode(
                fields, "routingCode", "Routing 코드", validation);
        String orderNumber = normalizeCode(
                fields, "orderNumber", "생산오더 번호", validation);
        Long quantity = parseLong(
                fields, "quantity", 1, 1_000_000, validation);
        OffsetDateTime releaseAt =
                parseDateTime(fields, "releaseAt", validation);
        OffsetDateTime dueAt =
                parseDateTime(fields, "dueAt", validation);
        Integer priority =
                parseInt(fields, "priority", 1, 100, validation);
        if (validation.hasErrors()) {
            return;
        }
        Routing routing = state.routing(productCode, routingCode);
        if (routing == null) {
            validation.reference(
                    "routingCode",
                    "앞선 유효 행이나 DB에서 Routing을 찾을 수 없습니다."
            );
            return;
        }
        if (state.existing().orderNumbers().contains(orderNumber)) {
            validation.duplicate("orderNumber", "기존 생산오더 번호입니다.");
            return;
        }
        if (state.orderNumbers().contains(orderNumber)) {
            validation.duplicate("orderNumber", "파일 안에서 생산오더 번호가 중복됩니다.");
            return;
        }
        try {
            ProductionOrder.create(
                    routing,
                    orderNumber,
                    quantity,
                    releaseAt,
                    dueAt,
                    priority
            );
            state.orderNumbers().add(orderNumber);
            validation.value("productCode", productCode);
            validation.value("routingCode", routingCode);
            validation.value("orderNumber", orderNumber);
            validation.value("quantity", quantity.toString());
            validation.value("releaseAt", releaseAt.toString());
            validation.value("dueAt", dueAt.toString());
            validation.value("priority", priority.toString());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            validation.invalid("row", exception.getMessage());
        }
    }

    private String normalizeCode(
            Map<String, String> fields,
            String field,
            String label,
            RowValidation validation
    ) {
        try {
            return BusinessCodeNormalizer.normalize(
                    fields.get(field),
                    label,
                    50
            );
        } catch (IllegalArgumentException exception) {
            validation.invalid(field, exception.getMessage());
            return null;
        }
    }

    private String required(
            Map<String, String> fields,
            String field,
            RowValidation validation
    ) {
        String value = fields.get(field);
        if (value == null || value.isBlank()) {
            validation.error(
                    field,
                    "REQUIRED",
                    field + "은(는) 필수입니다."
            );
            return null;
        }
        return value.trim();
    }

    private <T extends Enum<T>> T parseEnum(
            Map<String, String> fields,
            String field,
            Class<T> enumType,
            RowValidation validation
    ) {
        String value = required(fields, field, validation);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(
                    enumType,
                    value.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            validation.invalid(
                    field,
                    "허용값은 %s입니다."
                            .formatted(List.of(enumType.getEnumConstants()))
            );
            return null;
        }
    }

    private Integer parseInt(
            Map<String, String> fields,
            String field,
            int min,
            int max,
            RowValidation validation
    ) {
        String value = required(fields, field, validation);
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            validation.invalid(
                    field,
                    "%d 이상 %d 이하의 정수여야 합니다."
                            .formatted(min, max)
            );
            return null;
        }
    }

    private Long parseLong(
            Map<String, String> fields,
            String field,
            long min,
            long max,
            RowValidation validation
    ) {
        String value = required(fields, field, validation);
        if (value == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < min || parsed > max) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            validation.invalid(
                    field,
                    "%d 이상 %d 이하의 정수여야 합니다."
                            .formatted(min, max)
            );
            return null;
        }
    }

    private OffsetDateTime parseDateTime(
            Map<String, String> fields,
            String field,
            RowValidation validation
    ) {
        String value = required(fields, field, validation);
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            validation.invalid(
                    field,
                    "UTC offset을 포함한 ISO-8601 시각이어야 합니다."
            );
            return null;
        }
    }

    private static final class ValidationState {

        private final ExistingPlanningData existing;
        private final Map<String, Factory> factories = new HashMap<>();
        private final Map<String, ProductionLine> lines = new HashMap<>();
        private final Map<String, Machine> machines = new HashMap<>();
        private final Map<String, Product> products = new HashMap<>();
        private final Map<String, Routing> routings = new HashMap<>();
        private final Set<String> orderNumbers = new HashSet<>();

        private ValidationState(ExistingPlanningData existing) {
            this.existing = existing;
        }

        private Factory factory(String code) {
            return factories.getOrDefault(
                    code,
                    existing.factories().get(code)
            );
        }

        private ProductionLine line(
                String factoryCode,
                String lineCode
        ) {
            String key = PlanningDataKeys.line(factoryCode, lineCode);
            return lines.getOrDefault(key, existing.lines().get(key));
        }

        private Machine machine(
                String factoryCode,
                String lineCode,
                String machineCode
        ) {
            String key = PlanningDataKeys.machine(
                    factoryCode,
                    lineCode,
                    machineCode
            );
            return machines.getOrDefault(
                    key,
                    existing.machines().get(key)
            );
        }

        private Product product(String code) {
            return products.getOrDefault(
                    code,
                    existing.products().get(code)
            );
        }

        private Routing routing(
                String productCode,
                String routingCode
        ) {
            String key = PlanningDataKeys.routing(
                    productCode,
                    routingCode
            );
            return routings.getOrDefault(
                    key,
                    existing.routings().get(key)
            );
        }

        private ExistingPlanningData existing() {
            return existing;
        }

        private Map<String, Factory> factories() {
            return factories;
        }

        private Map<String, ProductionLine> lines() {
            return lines;
        }

        private Map<String, Machine> machines() {
            return machines;
        }

        private Map<String, Product> products() {
            return products;
        }

        private Map<String, Routing> routings() {
            return routings;
        }

        private Set<String> orderNumbers() {
            return orderNumbers;
        }
    }

    private static final class RowValidation {

        private final int rowNumber;
        private final Map<String, String> normalizedValues =
                new LinkedHashMap<>();
        private final List<PlanningDataImportRowError> errors =
                new ArrayList<>();

        private RowValidation(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        private void value(String field, String value) {
            normalizedValues.put(field, value);
        }

        private void duplicate(String field, String message) {
            error(field, "DUPLICATE", message);
        }

        private void reference(String field, String message) {
            error(field, "REFERENCE_NOT_FOUND", message);
        }

        private void invalid(String field, String message) {
            error(field, "INVALID_VALUE", message);
        }

        private void error(
                String field,
                String code,
                String message
        ) {
            errors.add(new PlanningDataImportRowError(
                    field,
                    code,
                    message
            ));
        }

        private boolean hasErrors() {
            return !errors.isEmpty();
        }

        private PlanningDataImportRowPreview preview(String type) {
            return new PlanningDataImportRowPreview(
                    rowNumber,
                    type,
                    errors.isEmpty(),
                    normalizedValues,
                    errors
            );
        }
    }
}
