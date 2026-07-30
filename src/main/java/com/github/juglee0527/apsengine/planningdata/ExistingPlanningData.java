package com.github.juglee0527.apsengine.planningdata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.routing.Routing;

record ExistingPlanningData(
        Map<String, Factory> factories,
        Map<String, ProductionLine> lines,
        Map<String, Machine> machines,
        Map<String, Product> products,
        Map<String, Routing> routings,
        Set<String> orderNumbers
) {

    static ExistingPlanningData from(
            List<Factory> factories,
            List<ProductionLine> lines,
            List<Machine> machines,
            List<Product> products,
            List<Routing> routings,
            List<ProductionOrder> orders
    ) {
        Map<String, Factory> factoriesByCode = new HashMap<>();
        for (Factory factory : factories) {
            factoriesByCode.put(factory.code(), factory);
        }
        Map<String, ProductionLine> linesByCode = new HashMap<>();
        for (ProductionLine line : lines) {
            linesByCode.put(
                    PlanningDataKeys.line(
                            line.factory().code(),
                            line.code()
                    ),
                    line
            );
        }
        Map<String, Machine> machinesByCode = new HashMap<>();
        for (Machine machine : machines) {
            ProductionLine line = machine.productionLine();
            machinesByCode.put(
                    PlanningDataKeys.machine(
                            line.factory().code(),
                            line.code(),
                            machine.code()
                    ),
                    machine
            );
        }
        Map<String, Product> productsByCode = new HashMap<>();
        for (Product product : products) {
            productsByCode.put(product.code(), product);
        }
        Map<String, Routing> routingsByCode = new HashMap<>();
        for (Routing routing : routings) {
            routingsByCode.put(
                    PlanningDataKeys.routing(
                            routing.product().code(),
                            routing.code()
                    ),
                    routing
            );
        }
        Set<String> orderNumbers = new HashSet<>();
        for (ProductionOrder order : orders) {
            orderNumbers.add(order.orderNumber());
        }
        return new ExistingPlanningData(
                Map.copyOf(factoriesByCode),
                Map.copyOf(linesByCode),
                Map.copyOf(machinesByCode),
                Map.copyOf(productsByCode),
                Map.copyOf(routingsByCode),
                Set.copyOf(orderNumbers)
        );
    }
}
