package com.github.juglee0527.apsengine.planningdata;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.machine.MachineStatus;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import org.springframework.stereotype.Component;

@Component
class PlanningDataImportApplier {

    private final FactoryRepository factoryRepository;
    private final ProductionLineRepository lineRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final RoutingRepository routingRepository;
    private final ProductionOrderRepository orderRepository;

    PlanningDataImportApplier(
            FactoryRepository factoryRepository,
            ProductionLineRepository lineRepository,
            MachineRepository machineRepository,
            ProductRepository productRepository,
            RoutingRepository routingRepository,
            ProductionOrderRepository orderRepository
    ) {
        this.factoryRepository = factoryRepository;
        this.lineRepository = lineRepository;
        this.machineRepository = machineRepository;
        this.productRepository = productRepository;
        this.routingRepository = routingRepository;
        this.orderRepository = orderRepository;
    }

    void apply(List<PlanningDataImportRowPreview> rows) {
        ExistingPlanningData existing = ExistingPlanningData.from(
                factoryRepository.findAll(),
                lineRepository.findAll(),
                machineRepository.findAll(),
                productRepository.findAll(),
                routingRepository.findAll(),
                orderRepository.findAll()
        );
        AppliedData applied = new AppliedData(existing);
        for (PlanningDataImportRowPreview row : rows) {
            PlanningDataType type = PlanningDataType.valueOf(row.type());
            try {
                applyRow(type, row.normalizedValues(), applied);
            } catch (RuntimeException exception) {
                throw new PlanningDataApplyException(
                        row.rowNumber(),
                        type,
                        "CSV %d행 반영에 실패했습니다: %s"
                                .formatted(
                                        row.rowNumber(),
                                        rootMessage(exception)
                                ),
                        exception
                );
            }
        }
    }

    private void applyRow(
            PlanningDataType type,
            Map<String, String> values,
            AppliedData applied
    ) {
        switch (type) {
            case FACTORY -> applyFactory(values, applied);
            case PRODUCTION_LINE -> applyLine(values, applied);
            case MACHINE -> applyMachine(values, applied);
            case PRODUCT -> applyProduct(values, applied);
            case ROUTING -> applyRouting(values, applied);
            case PRODUCTION_ORDER -> applyOrder(values, applied);
        }
    }

    private void applyFactory(
            Map<String, String> values,
            AppliedData applied
    ) {
        Factory factory = Factory.create(
                values.get("factoryCode"),
                values.get("name")
        );
        factoryRepository.saveAndFlush(factory);
        applied.factories().put(factory.code(), factory);
    }

    private void applyLine(
            Map<String, String> values,
            AppliedData applied
    ) {
        Factory factory = required(
                applied.factory(values.get("factoryCode")),
                "공장을 찾을 수 없습니다."
        );
        ProductionLine line = ProductionLine.create(
                factory,
                values.get("lineCode"),
                values.get("name")
        );
        lineRepository.saveAndFlush(line);
        applied.lines().put(
                PlanningDataKeys.line(factory.code(), line.code()),
                line
        );
    }

    private void applyMachine(
            Map<String, String> values,
            AppliedData applied
    ) {
        ProductionLine line = required(
                applied.line(
                        values.get("factoryCode"),
                        values.get("lineCode")
                ),
                "생산라인을 찾을 수 없습니다."
        );
        Machine machine = Machine.create(
                line,
                values.get("machineCode"),
                values.get("name"),
                MachineStatus.valueOf(values.get("status"))
        );
        machineRepository.saveAndFlush(machine);
        applied.machines().put(
                PlanningDataKeys.machine(
                        values.get("factoryCode"),
                        line.code(),
                        machine.code()
                ),
                machine
        );
    }

    private void applyProduct(
            Map<String, String> values,
            AppliedData applied
    ) {
        Product product = Product.create(
                values.get("productCode"),
                values.get("name"),
                ProductUnit.valueOf(values.get("unit"))
        );
        productRepository.saveAndFlush(product);
        applied.products().put(product.code(), product);
    }

    private void applyRouting(
            Map<String, String> values,
            AppliedData applied
    ) {
        String key = PlanningDataKeys.routing(
                values.get("productCode"),
                values.get("routingCode")
        );
        Routing routing = applied.routings().get(key);
        if (routing == null) {
            Product product = required(
                    applied.product(values.get("productCode")),
                    "품목을 찾을 수 없습니다."
            );
            routing = Routing.create(
                    product,
                    values.get("routingCode"),
                    values.get("name")
            );
            applied.routings().put(key, routing);
        }
        Machine machine = required(
                applied.machine(
                        values.get("factoryCode"),
                        values.get("lineCode"),
                        values.get("machineCode")
                ),
                "설비를 찾을 수 없습니다."
        );
        routing.addOperation(
                Integer.parseInt(values.get("operationSequence")),
                values.get("operationCode"),
                values.get("operationName"),
                Integer.parseInt(values.get("processingTimeMinutes")),
                machine
        );
        routingRepository.saveAndFlush(routing);
    }

    private void applyOrder(
            Map<String, String> values,
            AppliedData applied
    ) {
        Routing routing = required(
                applied.routing(
                        values.get("productCode"),
                        values.get("routingCode")
                ),
                "Routing을 찾을 수 없습니다."
        );
        ProductionOrder order = ProductionOrder.create(
                routing,
                values.get("orderNumber"),
                Long.parseLong(values.get("quantity")),
                OffsetDateTime.parse(values.get("releaseAt")),
                OffsetDateTime.parse(values.get("dueAt")),
                Integer.parseInt(values.get("priority"))
        );
        orderRepository.saveAndFlush(order);
    }

    private <T> T required(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null
                ? throwable.getClass().getSimpleName()
                : cause.getMessage();
    }

    private static final class AppliedData {

        private final ExistingPlanningData existing;
        private final Map<String, Factory> factories = new HashMap<>();
        private final Map<String, ProductionLine> lines = new HashMap<>();
        private final Map<String, Machine> machines = new HashMap<>();
        private final Map<String, Product> products = new HashMap<>();
        private final Map<String, Routing> routings = new HashMap<>();

        private AppliedData(ExistingPlanningData existing) {
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
    }
}
