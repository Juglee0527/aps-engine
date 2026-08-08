package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Operation;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;
import com.github.juglee0527.apsengine.support.PostgreSqlContainerIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Rollback
class ScheduledOperationSearchPostgreSqlIntegrationTest
        extends PostgreSqlContainerIntegrationTest {

    private static final OffsetDateTime START =
            OffsetDateTime.parse("2026-08-10T08:00:00+09:00");

    @Autowired FactoryRepository factoryRepository;
    @Autowired ProductionLineRepository lineRepository;
    @Autowired MachineRepository machineRepository;
    @Autowired ProductRepository productRepository;
    @Autowired RoutingRepository routingRepository;
    @Autowired ProductionOrderRepository orderRepository;
    @Autowired ScheduleRunRepository scheduleRunRepository;
    @Autowired ScheduledOperationRepository operationRepository;

    @Test
    void searchesWithoutOptionalFiltersUsingPostgreSqlTypedParameters() {
        ScheduleRun run = createScheduleRun();

        Page<ScheduledOperation> result = operationRepository.search(
                run.id(),
                false,
                null,
                false,
                null,
                false,
                null,
                "",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().productionOrder()
                .orderNumber()).isEqualTo("TASK-SEARCH-PO");
    }

    private ScheduleRun createScheduleRun() {
        Factory factory = factoryRepository.saveAndFlush(
                Factory.create("TASK-SEARCH-F", "Task Search Factory")
        );
        ProductionLine line = lineRepository.saveAndFlush(
                ProductionLine.create(
                        factory,
                        "TASK-SEARCH-L",
                        "Task Search Line"
                )
        );
        Machine machine = machineRepository.saveAndFlush(
                Machine.create(line, "TASK-SEARCH-M", "Task Search Machine")
        );
        Product product = productRepository.saveAndFlush(
                Product.create(
                        "TASK-SEARCH-P",
                        "Task Search Product",
                        ProductUnit.PIECE
                )
        );
        Routing routing = Routing.create(
                product,
                "TASK-SEARCH-R",
                "Task Search Routing"
        );
        routing.addOperation(1, "OP-10", "Processing", 10, machine);
        routing = routingRepository.saveAndFlush(routing);
        Operation operation = routing.operations().getFirst();
        ProductionOrder order = ProductionOrder.create(
                routing,
                "TASK-SEARCH-PO",
                1,
                START,
                START.plusHours(1),
                50
        );
        order.confirm();
        order = orderRepository.saveAndFlush(order);

        ScheduledTask task = new ScheduledTask(
                order.id(),
                order.orderNumber(),
                operation.id(),
                machine.id(),
                1,
                operation.code(),
                operation.name(),
                null,
                0,
                START,
                START.plusMinutes(10),
                10,
                order.dueAt(),
                false
        );
        ScheduleRun run = ScheduleRun.create(
                UUID.randomUUID(),
                new SchedulingPlan(
                        START,
                        START.plusMinutes(10),
                        List.of(task)
                ),
                START
        );
        run.addScheduledOperation(order, operation, machine, task);
        return scheduleRunRepository.saveAndFlush(run);
    }
}
