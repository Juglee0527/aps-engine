package com.github.juglee0527.apsengine.learning;

import java.util.Comparator;
import java.util.List;

import com.github.juglee0527.apsengine.capacity.WorkingCalendarRepository;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTimeRepository;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduleExecutionRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunRepository;

import org.springframework.stereotype.Component;

@Component
public class LearningScenarioResetter {

    private final LearningScenarioEntityRepository entityRepository;
    private final ScheduleExecutionRepository executionRepository;
    private final ScheduleRunRepository scheduleRunRepository;
    private final ProductionOrderRepository orderRepository;
    private final MachineMaintenanceRepository maintenanceRepository;
    private final ChangeoverTimeRepository changeoverRepository;
    private final RoutingRepository routingRepository;
    private final ProductRepository productRepository;
    private final WorkingCalendarRepository calendarRepository;
    private final MachineRepository machineRepository;
    private final ProductionLineRepository lineRepository;
    private final FactoryRepository factoryRepository;

    public LearningScenarioResetter(
            LearningScenarioEntityRepository entityRepository,
            ScheduleExecutionRepository executionRepository,
            ScheduleRunRepository scheduleRunRepository,
            ProductionOrderRepository orderRepository,
            MachineMaintenanceRepository maintenanceRepository,
            ChangeoverTimeRepository changeoverRepository,
            RoutingRepository routingRepository,
            ProductRepository productRepository,
            WorkingCalendarRepository calendarRepository,
            MachineRepository machineRepository,
            ProductionLineRepository lineRepository,
            FactoryRepository factoryRepository
    ) {
        this.entityRepository = entityRepository;
        this.executionRepository = executionRepository;
        this.scheduleRunRepository = scheduleRunRepository;
        this.orderRepository = orderRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.changeoverRepository = changeoverRepository;
        this.routingRepository = routingRepository;
        this.productRepository = productRepository;
        this.calendarRepository = calendarRepository;
        this.machineRepository = machineRepository;
        this.lineRepository = lineRepository;
        this.factoryRepository = factoryRepository;
    }

    public void reset(LearningScenarioInstance instance) {
        List<LearningScenarioEntity> entities = entityRepository
                .findAllByScenarioInstance_IdOrderByIdDesc(instance.id())
                .stream()
                .sorted(Comparator.comparingInt(
                        entity -> deletionOrder(entity.entityType())
                ))
                .toList();
        for (LearningScenarioEntity entity : entities) {
            delete(entity.entityType(), entity.entityId());
        }
        entityRepository.deleteAllByScenarioInstance_Id(instance.id());
    }

    private void delete(LearningScenarioEntityType type, long id) {
        switch (type) {
            case SCHEDULE_EXECUTION -> deleteExecutionAndResult(id);
            case SCHEDULE_RUN -> scheduleRunRepository.deleteById(id);
            case PRODUCTION_ORDER -> orderRepository.deleteById(id);
            case MAINTENANCE -> maintenanceRepository.deleteById(id);
            case CHANGEOVER_TIME -> changeoverRepository.deleteById(id);
            case ROUTING -> routingRepository.deleteById(id);
            case PRODUCT -> productRepository.deleteById(id);
            case WORKING_CALENDAR -> calendarRepository.deleteById(id);
            case MACHINE -> machineRepository.deleteById(id);
            case PRODUCTION_LINE -> lineRepository.deleteById(id);
            case FACTORY -> factoryRepository.deleteById(id);
        }
    }

    private void deleteExecutionAndResult(long executionId) {
        Long resultScheduleRunId = executionRepository.findById(executionId)
                .map(execution -> execution.resultScheduleRunId())
                .orElse(null);
        executionRepository.deleteById(executionId);
        executionRepository.flush();
        if (resultScheduleRunId != null) {
            scheduleRunRepository.deleteById(resultScheduleRunId);
        }
    }

    private int deletionOrder(LearningScenarioEntityType type) {
        return switch (type) {
            case SCHEDULE_EXECUTION -> 0;
            case SCHEDULE_RUN -> 1;
            case PRODUCTION_ORDER -> 2;
            case MAINTENANCE, CHANGEOVER_TIME -> 3;
            case ROUTING -> 4;
            case PRODUCT, WORKING_CALENDAR -> 5;
            case MACHINE -> 6;
            case PRODUCTION_LINE -> 7;
            case FACTORY -> 8;
        };
    }
}
