package com.github.juglee0527.apsengine.learning;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearningScenarioResetterTest {

    @Mock LearningScenarioEntityRepository entityRepository;
    @Mock ScheduleExecutionRepository executionRepository;
    @Mock ScheduleRunRepository scheduleRunRepository;
    @Mock ProductionOrderRepository orderRepository;
    @Mock MachineMaintenanceRepository maintenanceRepository;
    @Mock ChangeoverTimeRepository changeoverRepository;
    @Mock RoutingRepository routingRepository;
    @Mock ProductRepository productRepository;
    @Mock WorkingCalendarRepository calendarRepository;
    @Mock MachineRepository machineRepository;
    @Mock ProductionLineRepository lineRepository;
    @Mock FactoryRepository factoryRepository;

    @Test
    void deletesOnlyTrackedEntitiesInForeignKeySafeOrder() {
        LearningScenarioInstance instance = LearningScenarioInstance.create(
                UUID.randomUUID(),
                "FIRST_PLAN",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(instance, "id", 4L);
        when(entityRepository.findAllByScenarioInstance_IdOrderByIdDesc(4L))
                .thenReturn(List.of(
                        LearningScenarioEntity.create(
                                instance,
                                LearningScenarioEntityType.FACTORY,
                                11L
                        ),
                        LearningScenarioEntity.create(
                                instance,
                                LearningScenarioEntityType.PRODUCTION_ORDER,
                                22L
                        ),
                        LearningScenarioEntity.create(
                                instance,
                                LearningScenarioEntityType.SCHEDULE_EXECUTION,
                                33L
                        )
                ));
        LearningScenarioResetter resetter = new LearningScenarioResetter(
                entityRepository,
                executionRepository,
                scheduleRunRepository,
                orderRepository,
                maintenanceRepository,
                changeoverRepository,
                routingRepository,
                productRepository,
                calendarRepository,
                machineRepository,
                lineRepository,
                factoryRepository
        );

        resetter.reset(instance);

        InOrder order = inOrder(
                executionRepository,
                orderRepository,
                factoryRepository,
                entityRepository
        );
        order.verify(executionRepository).deleteById(33L);
        order.verify(orderRepository).deleteById(22L);
        order.verify(factoryRepository).deleteById(11L);
        order.verify(entityRepository).deleteAllByScenarioInstance_Id(4L);
    }
}
