package com.github.juglee0527.apsengine.learning;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenance;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderStatus;
import com.github.juglee0527.apsengine.product.routing.Routing;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;
import com.github.juglee0527.apsengine.scheduling.DispatchingRule;
import com.github.juglee0527.apsengine.scheduling.ScheduleRun;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunResponse;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunService;
import com.github.juglee0527.apsengine.scheduling.ScheduledOperationResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrozenHorizonLabService {

    private static final String SCENARIO_KEY = "FROZEN_HORIZON";
    private static final String URGENT_SUFFIX = "-FH-URGENT";
    private static final String CANCEL_SUFFIX = "-FH-CANCEL";

    private final LearningScenarioService scenarioService;
    private final LearningScenarioEntityRepository entityRepository;
    private final LearningScenarioEntityTracker tracker;
    private final ProductionOrderRepository orderRepository;
    private final RoutingRepository routingRepository;
    private final MachineRepository machineRepository;
    private final MachineMaintenanceRepository maintenanceRepository;
    private final ScheduleRunService scheduleRunService;

    public FrozenHorizonLabService(
            LearningScenarioService scenarioService,
            LearningScenarioEntityRepository entityRepository,
            LearningScenarioEntityTracker tracker,
            ProductionOrderRepository orderRepository,
            RoutingRepository routingRepository,
            MachineRepository machineRepository,
            MachineMaintenanceRepository maintenanceRepository,
            ScheduleRunService scheduleRunService
    ) {
        this.scenarioService = scenarioService;
        this.entityRepository = entityRepository;
        this.tracker = tracker;
        this.orderRepository = orderRepository;
        this.routingRepository = routingRepository;
        this.machineRepository = machineRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.scheduleRunService = scheduleRunService;
    }

    @Transactional
    public FrozenHorizonLabResponse run(
            long instanceId,
            FrozenHorizonLabRequest request
    ) {
        LearningScenarioPlanScope initialScope =
                scenarioService.planScope(instanceId);
        requireFrozenHorizon(initialScope.instance());
        DispatchingRule rule = request.dispatchingRule() == null
                ? DispatchingRule.EXPLICIT_PRIORITY
                : request.dispatchingRule();

        List<Long> baselineOrderIds = initialScope.productionOrderIds()
                .stream()
                .filter(id -> !requiredOrder(id).orderNumber()
                        .endsWith(URGENT_SUFFIX))
                .toList();
        ScheduleRun baseline = scheduleRunService.execute(
                request.baselineExecutionKey(),
                initialScope.planningStart(),
                DispatchingRule.EXPLICIT_PRIORITY,
                baselineOrderIds
        );
        trackRun(initialScope.instance(), baseline.id());

        OffsetDateTime frozenAt = initialScope.planningStart().plusHours(2);
        OffsetDateTime maintenanceStart =
                initialScope.planningStart().plusHours(3);
        OffsetDateTime maintenanceEnd =
                initialScope.planningStart().plusHours(5);
        cancelPlannedOrder(baselineOrderIds);
        createMaintenanceIfMissing(
                initialScope.instance(),
                maintenanceStart,
                maintenanceEnd
        );
        ProductionOrder urgent = createUrgentOrderIfMissing(
                initialScope.instance(),
                baselineOrderIds,
                frozenAt
        );

        List<Long> rescheduleScope = new ArrayList<>(baselineOrderIds);
        rescheduleScope.add(urgent.id());
        ScheduleRun rescheduled = scheduleRunService.reschedule(
                baseline.id(),
                request.rescheduleExecutionKey(),
                frozenAt,
                rule,
                rescheduleScope
        );
        trackRun(initialScope.instance(), rescheduled.id());

        ScheduleRunResponse before = ScheduleRunResponse.from(baseline);
        ScheduleRunResponse after = ScheduleRunResponse.from(rescheduled);
        return new FrozenHorizonLabResponse(
                frozenAt,
                maintenanceStart,
                maintenanceEnd,
                before,
                after,
                classify(before, after, frozenAt),
                "동결 경계 전에 시작한 작업은 경계와 겹쳐도 고정됩니다. "
                        + "취소 오더의 미래 작업은 제외하고, 정비 이후에 긴급오더와 남은 작업만 다시 배치했습니다."
        );
    }

    private void requireFrozenHorizon(LearningScenarioInstance instance) {
        if (!SCENARIO_KEY.equals(instance.scenarioKey())) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Frozen Horizon 학습 인스턴스에서만 실행할 수 있습니다."
            );
        }
    }

    private void cancelPlannedOrder(List<Long> orderIds) {
        orderIds.stream()
                .map(this::requiredOrder)
                .filter(order -> order.orderNumber().endsWith(CANCEL_SUFFIX))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.INVALID_REQUEST,
                        "취소 실습용 생산오더가 없습니다."
                ))
                .cancel();
    }

    private void createMaintenanceIfMissing(
            LearningScenarioInstance instance,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (!trackedIds(instance.id(), LearningScenarioEntityType.MAINTENANCE)
                .isEmpty()) {
            return;
        }
        long machineId = trackedIds(
                instance.id(),
                LearningScenarioEntityType.MACHINE
        ).getFirst();
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.MACHINE_NOT_FOUND
                ));
        MachineMaintenance maintenance = maintenanceRepository.saveAndFlush(
                MachineMaintenance.create(
                        machine,
                        startAt,
                        endAt,
                        "긴급 재계획 중 발견된 계획 정비"
                )
        );
        tracker.track(
                instance,
                LearningScenarioEntityType.MAINTENANCE,
                maintenance.id()
        );
    }

    private ProductionOrder createUrgentOrderIfMissing(
            LearningScenarioInstance instance,
            List<Long> baselineOrderIds,
            OffsetDateTime frozenAt
    ) {
        ProductionOrder existing = trackedIds(
                instance.id(),
                LearningScenarioEntityType.PRODUCTION_ORDER
        ).stream()
                .map(this::requiredOrder)
                .filter(order -> order.orderNumber().endsWith(URGENT_SUFFIX))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        ProductionOrder source = requiredOrder(baselineOrderIds.getFirst());
        Routing routing = routingRepository.findDetailById(
                source.routing().id()
        ).orElseThrow(() -> new ApplicationException(
                ErrorCode.ROUTING_NOT_FOUND
        ));
        ProductionOrder urgent = ProductionOrder.create(
                routing,
                instance.namespace() + URGENT_SUFFIX,
                1,
                frozenAt,
                frozenAt.plusHours(4),
                100
        );
        urgent.confirm();
        urgent = orderRepository.saveAndFlush(urgent);
        tracker.track(
                instance,
                LearningScenarioEntityType.PRODUCTION_ORDER,
                urgent.id()
        );
        return urgent;
    }

    private List<FrozenHorizonTaskChange> classify(
            ScheduleRunResponse before,
            ScheduleRunResponse after,
            OffsetDateTime frozenAt
    ) {
        Map<String, ScheduledOperationResponse> afterByKey = new HashMap<>();
        after.tasks().forEach(task -> afterByKey.put(key(task), task));
        List<FrozenHorizonTaskChange> changes = new ArrayList<>();
        for (ScheduledOperationResponse task : before.tasks()) {
            ScheduledOperationResponse replacement = afterByKey.remove(key(task));
            String classification;
            String reason;
            if (task.startAt().isBefore(frozenAt)) {
                classification = "FIXED";
                reason = "동결 경계 전에 시작해 기존 시각을 보호했습니다.";
            } else if (replacement == null) {
                classification = "EXCLUDED";
                reason = "취소된 오더의 아직 시작하지 않은 작업이라 제외했습니다.";
            } else {
                classification = "MOVED";
                reason = "동결 경계 이후 작업이라 정비와 긴급오더를 반영해 이동했습니다.";
            }
            changes.add(change(classification, task, replacement, reason));
        }
        afterByKey.values().stream()
                .sorted((left, right) -> left.startAt().compareTo(right.startAt()))
                .map(task -> change(
                        "NEW",
                        null,
                        task,
                        "재계획 시점에 확정된 긴급오더를 새로 배치했습니다."
                ))
                .forEach(changes::add);
        return List.copyOf(changes);
    }

    private FrozenHorizonTaskChange change(
            String classification,
            ScheduledOperationResponse before,
            ScheduledOperationResponse after,
            String reason
    ) {
        ScheduledOperationResponse reference = before == null ? after : before;
        return new FrozenHorizonTaskChange(
                classification,
                reference.orderNumber(),
                reference.operationCode(),
                before == null ? null : before.startAt(),
                before == null ? null : before.endAt(),
                after == null ? null : after.startAt(),
                after == null ? null : after.endAt(),
                reason
        );
    }

    private String key(ScheduledOperationResponse task) {
        return task.productionOrderId() + ":" + task.operationId();
    }

    private ProductionOrder requiredOrder(long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.PRODUCTION_ORDER_NOT_FOUND
                ));
    }

    private List<Long> trackedIds(
            long instanceId,
            LearningScenarioEntityType type
    ) {
        return entityRepository
                .findAllByScenarioInstance_IdAndEntityTypeOrderByEntityIdAsc(
                        instanceId,
                        type
                )
                .stream()
                .map(LearningScenarioEntity::entityId)
                .toList();
    }

    private void trackRun(
            LearningScenarioInstance instance,
            long scheduleRunId
    ) {
        if (!entityRepository
                .existsByScenarioInstance_IdAndEntityTypeAndEntityId(
                        instance.id(),
                        LearningScenarioEntityType.SCHEDULE_RUN,
                        scheduleRunId
                )) {
            tracker.track(
                    instance,
                    LearningScenarioEntityType.SCHEDULE_RUN,
                    scheduleRunId
            );
        }
    }
}
