package com.github.juglee0527.apsengine.learning;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.juglee0527.apsengine.capacity.WorkingCalendar;
import com.github.juglee0527.apsengine.capacity.WorkingCalendarRepository;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTime;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTimeRepository;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenance;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.MachineSpec;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.OperationSpec;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.OrderSpec;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.ProductSpec;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import org.springframework.stereotype.Component;

@Component
class LearningScenarioProvisioner {

    private final FactoryRepository factoryRepository;
    private final ProductionLineRepository lineRepository;
    private final MachineRepository machineRepository;
    private final WorkingCalendarRepository calendarRepository;
    private final ProductRepository productRepository;
    private final RoutingRepository routingRepository;
    private final ProductionOrderRepository orderRepository;
    private final ChangeoverTimeRepository changeoverRepository;
    private final MachineMaintenanceRepository maintenanceRepository;
    private final LearningScenarioEntityTracker tracker;

    LearningScenarioProvisioner(
            FactoryRepository factoryRepository,
            ProductionLineRepository lineRepository,
            MachineRepository machineRepository,
            WorkingCalendarRepository calendarRepository,
            ProductRepository productRepository,
            RoutingRepository routingRepository,
            ProductionOrderRepository orderRepository,
            ChangeoverTimeRepository changeoverRepository,
            MachineMaintenanceRepository maintenanceRepository,
            LearningScenarioEntityTracker tracker
    ) {
        this.factoryRepository = factoryRepository;
        this.lineRepository = lineRepository;
        this.machineRepository = machineRepository;
        this.calendarRepository = calendarRepository;
        this.productRepository = productRepository;
        this.routingRepository = routingRepository;
        this.orderRepository = orderRepository;
        this.changeoverRepository = changeoverRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.tracker = tracker;
    }

    void provision(
            LearningScenarioInstance instance,
            LearningScenarioBlueprint blueprint
    ) {
        String namespace = instance.namespace();
        Factory factory = factoryRepository.saveAndFlush(Factory.create(
                namespace + "-F",
                blueprint.title() + " 실습 공장"
        ));
        track(instance, LearningScenarioEntityType.FACTORY, factory.id());

        ProductionLine line = lineRepository.saveAndFlush(
                ProductionLine.create(
                        factory,
                        namespace + "-L",
                        "APS 학습 라인"
                )
        );
        track(
                instance,
                LearningScenarioEntityType.PRODUCTION_LINE,
                line.id()
        );

        Map<String, Machine> machines = createMachines(
                instance,
                blueprint,
                line
        );
        Map<String, Routing> routings = createProductsAndRoutings(
                instance,
                blueprint,
                machines
        );
        createConstraints(instance, blueprint, machines, routings);
        createOrders(instance, blueprint, routings);
    }

    private Map<String, Machine> createMachines(
            LearningScenarioInstance instance,
            LearningScenarioBlueprint blueprint,
            ProductionLine line
    ) {
        Map<String, Machine> machines = new LinkedHashMap<>();
        for (MachineSpec spec : blueprint.machines()) {
            Machine machine = machineRepository.saveAndFlush(Machine.create(
                    line,
                    instance.namespace() + "-" + spec.code(),
                    spec.name()
            ));
            machines.put(spec.code(), machine);
            track(
                    instance,
                    LearningScenarioEntityType.MACHINE,
                    machine.id()
            );
            createWeekdayCalendar(instance, machine);
        }
        return machines;
    }

    private void createWeekdayCalendar(
            LearningScenarioInstance instance,
            Machine machine
    ) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                continue;
            }
            WorkingCalendar calendar = calendarRepository.saveAndFlush(
                    WorkingCalendar.create(
                            machine,
                            day,
                            LocalTime.of(8, 0),
                            LocalTime.of(17, 0)
                    )
            );
            track(
                    instance,
                    LearningScenarioEntityType.WORKING_CALENDAR,
                    calendar.id()
            );
        }
    }

    private Map<String, Routing> createProductsAndRoutings(
            LearningScenarioInstance instance,
            LearningScenarioBlueprint blueprint,
            Map<String, Machine> machines
    ) {
        Map<String, Routing> routings = new LinkedHashMap<>();
        for (ProductSpec spec : blueprint.products()) {
            Product product = productRepository.saveAndFlush(Product.create(
                    instance.namespace() + "-" + spec.code(),
                    spec.name(),
                    ProductUnit.PIECE
            ));
            track(
                    instance,
                    LearningScenarioEntityType.PRODUCT,
                    product.id()
            );
            Routing routing = Routing.create(
                    product,
                    "STD",
                    spec.name() + " 표준 공정"
            );
            for (OperationSpec operation : spec.operations()) {
                Map<Machine, Integer> candidates = new LinkedHashMap<>();
                operation.machineCandidates().forEach((code, priority) ->
                        candidates.put(
                                requiredMachine(machines, code),
                                priority
                        ));
                routing.addOperation(
                        operation.sequence(),
                        operation.code(),
                        operation.name(),
                        operation.processingMinutes(),
                        requiredMachine(machines, operation.machineCode()),
                        candidates
                );
            }
            routing = routingRepository.saveAndFlush(routing);
            routings.put(spec.code(), routing);
            track(
                    instance,
                    LearningScenarioEntityType.ROUTING,
                    routing.id()
            );
        }
        return routings;
    }

    private void createConstraints(
            LearningScenarioInstance instance,
            LearningScenarioBlueprint blueprint,
            Map<String, Machine> machines,
            Map<String, Routing> routings
    ) {
        if (blueprint.key().equals("CHANGEOVER")) {
            Product itemA = requiredRouting(routings, "ITEM-A").product();
            Product itemB = requiredRouting(routings, "ITEM-B").product();
            createChangeover(
                    instance,
                    requiredMachine(machines, "CELL"),
                    itemA,
                    itemB,
                    120
            );
            createChangeover(
                    instance,
                    requiredMachine(machines, "CELL"),
                    itemB,
                    itemA,
                    15
            );
        }
        if (blueprint.key().equals("MAINTENANCE")) {
            MachineMaintenance maintenance =
                    maintenanceRepository.saveAndFlush(
                            MachineMaintenance.create(
                                    requiredMachine(machines, "PRESS"),
                                    instance.planningStart().plusHours(2),
                                    instance.planningStart().plusHours(5),
                                    "APS 학습용 계획 정비"
                            )
                    );
            track(
                    instance,
                    LearningScenarioEntityType.MAINTENANCE,
                    maintenance.id()
            );
        }
    }

    private void createChangeover(
            LearningScenarioInstance instance,
            Machine machine,
            Product fromProduct,
            Product toProduct,
            int minutes
    ) {
        ChangeoverTime changeover = changeoverRepository.saveAndFlush(
                ChangeoverTime.create(
                        machine,
                        fromProduct,
                        toProduct,
                        minutes
                )
        );
        track(
                instance,
                LearningScenarioEntityType.CHANGEOVER_TIME,
                changeover.id()
        );
    }

    private void createOrders(
            LearningScenarioInstance instance,
            LearningScenarioBlueprint blueprint,
            Map<String, Routing> routings
    ) {
        for (OrderSpec spec : blueprint.orders()) {
            ProductionOrder order = ProductionOrder.create(
                    requiredRouting(routings, spec.productCode()),
                    instance.namespace() + "-" + spec.orderNumber(),
                    spec.quantity(),
                    instance.planningStart().plusMinutes(
                            spec.releaseOffsetMinutes()
                    ),
                    instance.planningStart().plusMinutes(
                            spec.dueOffsetMinutes()
                    ),
                    spec.priority()
            );
            order.confirm();
            order = orderRepository.saveAndFlush(order);
            track(
                    instance,
                    LearningScenarioEntityType.PRODUCTION_ORDER,
                    order.id()
            );
        }
    }

    private Machine requiredMachine(
            Map<String, Machine> machines,
            String code
    ) {
        Machine machine = machines.get(code);
        if (machine == null) {
            throw new IllegalStateException("시나리오 설비 정의가 없습니다: " + code);
        }
        return machine;
    }

    private Routing requiredRouting(
            Map<String, Routing> routings,
            String productCode
    ) {
        Routing routing = routings.get(productCode);
        if (routing == null) {
            throw new IllegalStateException(
                    "시나리오 Routing 정의가 없습니다: " + productCode
            );
        }
        return routing;
    }

    private void track(
            LearningScenarioInstance instance,
            LearningScenarioEntityType type,
            Long id
    ) {
        tracker.track(instance, type, id);
    }
}
