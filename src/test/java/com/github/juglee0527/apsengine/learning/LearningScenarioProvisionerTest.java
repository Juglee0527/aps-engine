package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.github.juglee0527.apsengine.capacity.WorkingCalendar;
import com.github.juglee0527.apsengine.capacity.WorkingCalendarRepository;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTimeRepository;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTime;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenance;
import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderStatus;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearningScenarioProvisionerTest {

    @Mock FactoryRepository factoryRepository;
    @Mock ProductionLineRepository lineRepository;
    @Mock MachineRepository machineRepository;
    @Mock WorkingCalendarRepository calendarRepository;
    @Mock ProductRepository productRepository;
    @Mock RoutingRepository routingRepository;
    @Mock ProductionOrderRepository orderRepository;
    @Mock ChangeoverTimeRepository changeoverRepository;
    @Mock MachineMaintenanceRepository maintenanceRepository;
    @Mock LearningScenarioEntityTracker tracker;

    private LearningScenarioProvisioner provisioner;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        stubIdentity(factoryRepository, Factory.class);
        stubIdentity(lineRepository, ProductionLine.class);
        stubIdentity(machineRepository, Machine.class);
        stubIdentity(calendarRepository, WorkingCalendar.class);
        stubIdentity(productRepository, Product.class);
        stubIdentity(routingRepository, Routing.class);
        stubIdentity(orderRepository, ProductionOrder.class);
        provisioner = new LearningScenarioProvisioner(
                factoryRepository,
                lineRepository,
                machineRepository,
                calendarRepository,
                productRepository,
                routingRepository,
                orderRepository,
                changeoverRepository,
                maintenanceRepository,
                tracker
        );
    }

    @Test
    void provisionsAndTracksDeterministicFirstPlanData() {
        LearningScenarioCatalog catalog = new LearningScenarioCatalog();
        LearningScenarioInstance instance = LearningScenarioInstance.create(
                UUID.fromString("a55ab6a9-f408-4286-9783-f4e408a59ff4"),
                "FIRST_PLAN",
                OffsetDateTime.parse("2026-08-10T08:00:00+09:00"),
                OffsetDateTime.parse("2026-08-08T10:00:00+09:00")
        );

        provisioner.provision(instance, catalog.blueprint("FIRST_PLAN"));

        verify(machineRepository, times(2)).saveAndFlush(any());
        verify(calendarRepository, times(10)).saveAndFlush(any());
        verify(productRepository, times(2)).saveAndFlush(any());
        verify(routingRepository, times(2)).saveAndFlush(any());
        verify(orderRepository, times(4)).saveAndFlush(any());
        verify(tracker, times(22)).track(
                any(),
                any(),
                org.mockito.ArgumentMatchers.anyLong()
        );
        verifyNoInteractions(changeoverRepository, maintenanceRepository);

        ArgumentCaptor<ProductionOrder> orders =
                ArgumentCaptor.forClass(ProductionOrder.class);
        verify(orderRepository, times(4)).saveAndFlush(orders.capture());
        assertThat(orders.getAllValues())
                .allSatisfy(order -> {
                    assertThat(order.status())
                            .isEqualTo(ProductionOrderStatus.CONFIRMED);
                    assertThat(order.orderNumber())
                            .startsWith(instance.namespace());
                    assertThat(order.dueAt()).isAfter(order.releaseAt());
                });
    }

    @Test
    void provisionsDirectionalChangeoverConstraints() {
        stubIdentity(changeoverRepository, ChangeoverTime.class);
        LearningScenarioInstance instance = scenarioInstance("CHANGEOVER");

        provisioner.provision(
                instance,
                new LearningScenarioCatalog().blueprint("CHANGEOVER")
        );

        ArgumentCaptor<ChangeoverTime> values =
                ArgumentCaptor.forClass(ChangeoverTime.class);
        verify(changeoverRepository, times(2))
                .saveAndFlush(values.capture());
        assertThat(values.getAllValues())
                .extracting(ChangeoverTime::changeoverMinutes)
                .containsExactly(120, 15);
    }

    @Test
    void provisionsMaintenanceRelativeToPlanningStart() {
        stubIdentity(maintenanceRepository, MachineMaintenance.class);
        LearningScenarioInstance instance = scenarioInstance("MAINTENANCE");

        provisioner.provision(
                instance,
                new LearningScenarioCatalog().blueprint("MAINTENANCE")
        );

        ArgumentCaptor<MachineMaintenance> value =
                ArgumentCaptor.forClass(MachineMaintenance.class);
        verify(maintenanceRepository).saveAndFlush(value.capture());
        assertThat(value.getValue().startAt())
                .isEqualTo(instance.planningStart().plusHours(2));
        assertThat(value.getValue().endAt())
                .isEqualTo(instance.planningStart().plusHours(5));
    }

    private LearningScenarioInstance scenarioInstance(String key) {
        return LearningScenarioInstance.create(
                UUID.randomUUID(),
                key,
                OffsetDateTime.parse("2026-08-10T08:00:00+09:00"),
                OffsetDateTime.parse("2026-08-08T10:00:00+09:00")
        );
    }

    private <T> void stubIdentity(
            org.springframework.data.jpa.repository.JpaRepository<T, Long> repository,
            Class<T> type
    ) {
        when(repository.saveAndFlush(any(type))).thenAnswer(invocation -> {
            T entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", ids.getAndIncrement());
            return entity;
        });
    }
}
