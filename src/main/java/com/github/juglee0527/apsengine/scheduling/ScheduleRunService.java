package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.juglee0527.apsengine.capacity.WeeklyWorkingTime;
import com.github.juglee0527.apsengine.capacity.WorkingCalendar;
import com.github.juglee0527.apsengine.capacity.WorkingCalendarRepository;
import com.github.juglee0527.apsengine.capacity.UnavailableInterval;
import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTime;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTimeRepository;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenance;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineStatus;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderStatus;
import com.github.juglee0527.apsengine.product.routing.Operation;
import com.github.juglee0527.apsengine.product.routing.OperationMachineCandidate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleRunService {

    private final ProductionOrderRepository productionOrderRepository;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final ChangeoverTimeRepository changeoverTimeRepository;
    private final MachineMaintenanceRepository maintenanceRepository;
    private final ScheduleRunRepository scheduleRunRepository;
    private final ScheduleKpiCalculator kpiCalculator;

    public ScheduleRunService(
            ProductionOrderRepository productionOrderRepository,
            WorkingCalendarRepository workingCalendarRepository,
            ChangeoverTimeRepository changeoverTimeRepository,
            MachineMaintenanceRepository maintenanceRepository,
            ScheduleRunRepository scheduleRunRepository
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.workingCalendarRepository = workingCalendarRepository;
        this.changeoverTimeRepository = changeoverTimeRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.scheduleRunRepository = scheduleRunRepository;
        this.kpiCalculator = new ScheduleKpiCalculator();
    }

    @Transactional
    public ScheduleRun execute(
            UUID executionKey,
            OffsetDateTime planningStart
    ) {
        return execute(
                executionKey,
                planningStart,
                DispatchingRule.EXPLICIT_PRIORITY
        );
    }

    @Transactional
    public ScheduleRun execute(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule
    ) {
        return execute(
                executionKey,
                planningStart,
                dispatchingRule,
                null
        );
    }

    @Transactional
    public ScheduleRun execute(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule,
            List<Long> productionOrderIds
    ) {
        if (executionKey == null
                || planningStart == null
                || dispatchingRule == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        ScheduleRun existing = scheduleRunRepository
                .findByExecutionKey(executionKey)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        List<ProductionOrder> orders = findPlanningOrders(
                productionOrderIds
        );
        if (orders.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.CONFIRMED_PRODUCTION_ORDER_REQUIRED
            );
        }

        SchedulingContext context = createContext(orders, planningStart);
        ScheduleCalculation calculation = calculateSchedule(
                planningStart,
                dispatchingRule,
                context,
                FrozenScheduleSeed.empty()
        );

        ScheduleRun scheduleRun = ScheduleRun.create(
                executionKey,
                calculation.plan(),
                OffsetDateTime.now(),
                dispatchingRule,
                calculation.kpis()
        );
        addScheduledOperations(
                scheduleRun,
                calculation.plan().tasks(),
                context.ordersById(),
                context.operationsById(),
                context.machinesById()
        );
        for (ProductionOrder order : orders) {
            order.markScheduled();
        }
        return saveScheduleRun(scheduleRun);
    }

    @Transactional(readOnly = true)
    public DispatchingRuleComparisonResponse compareDispatchingRules(
            OffsetDateTime planningStart,
            List<Long> productionOrderIds
    ) {
        if (planningStart == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        List<ProductionOrder> orders = findPlanningOrders(
                productionOrderIds
        );
        if (orders.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.CONFIRMED_PRODUCTION_ORDER_REQUIRED
            );
        }
        SchedulingContext context = createContext(orders, planningStart);
        List<DispatchingRuleComparisonResult> results = List.of(
                DispatchingRule.EXPLICIT_PRIORITY,
                DispatchingRule.EDD,
                DispatchingRule.SPT
        ).stream().map(rule -> comparisonResult(
                rule,
                calculateSchedule(
                        planningStart,
                        rule,
                        context,
                        FrozenScheduleSeed.empty()
                )
        )).toList();
        DispatchingRuleComparisonResult recommended = results.stream()
                .min(Comparator
                        .comparingLong(
                                DispatchingRuleComparisonResult
                                        ::totalTardinessMinutes
                        )
                        .thenComparingInt(
                                DispatchingRuleComparisonResult
                                        ::delayedOrderCount
                        )
                        .thenComparingLong(
                                DispatchingRuleComparisonResult
                                        ::makespanMinutes
                        )
                        .thenComparingInt(result ->
                                ruleOrder(result.dispatchingRule())))
                .orElseThrow();
        return new DispatchingRuleComparisonResponse(
                recommended.dispatchingRule(),
                "총 지연시간, 지연 오더 수, Makespan 순으로 비교했습니다. "
                        + "동률이면 Priority, EDD, SPT 순으로 결정합니다.",
                results
        );
    }

    @Transactional(readOnly = true)
    public ConstraintImpactResponse compareConstraintImpact(
            String scenarioKey,
            OffsetDateTime planningStart,
            List<Long> productionOrderIds
    ) {
        List<ProductionOrder> orders = findPlanningOrders(
                productionOrderIds
        );
        SchedulingContext constrained = createContext(orders, planningStart);
        ScheduleCalculation withoutConstraint = calculateSchedule(
                planningStart,
                DispatchingRule.EXPLICIT_PRIORITY,
                withoutManufacturingConstraints(constrained),
                FrozenScheduleSeed.empty()
        );
        ScheduleCalculation withConstraint = calculateSchedule(
                planningStart,
                DispatchingRule.EXPLICIT_PRIORITY,
                constrained,
                FrozenScheduleSeed.empty()
        );
        return new ConstraintImpactResponse(
                scenarioKey,
                comparisonResult(
                        DispatchingRule.EXPLICIT_PRIORITY,
                        withoutConstraint
                ),
                comparisonResult(
                        DispatchingRule.EXPLICIT_PRIORITY,
                        withConstraint
                ),
                "전환시간, 정비 비가용시간, 대체 설비 후보를 제거한 기준 계획과 "
                        + "제약을 적용한 계획을 비교했습니다."
        );
    }

    private SchedulingContext withoutManufacturingConstraints(
            SchedulingContext context
    ) {
        List<SchedulingOrderInput> inputs = context.inputs().stream()
                .map(order -> new SchedulingOrderInput(
                        order.orderId(),
                        order.orderNumber(),
                        order.productId(),
                        order.quantity(),
                        order.releaseAt(),
                        order.dueAt(),
                        order.priority(),
                        order.operations().stream()
                                .map(this::withoutOperationConstraints)
                                .toList()
                ))
                .toList();
        Map<Long, SchedulingMachineCandidateInput> primaryCapacity =
                new LinkedHashMap<>();
        inputs.stream()
                .flatMap(order -> order.operations().stream())
                .flatMap(operation ->
                        operation.machineCandidates().stream())
                .forEach(candidate -> primaryCapacity.putIfAbsent(
                        candidate.machineId(),
                        candidate
                ));
        return new SchedulingContext(
                inputs,
                List.of(),
                List.copyOf(primaryCapacity.values()),
                context.ordersById(),
                context.operationsById(),
                context.machinesById()
        );
    }

    private SchedulingOperationInput withoutOperationConstraints(
            SchedulingOperationInput operation
    ) {
        SchedulingMachineCandidateInput primary = operation
                .machineCandidates()
                .stream()
                .filter(candidate -> candidate.machineId()
                        == operation.machineId())
                .findFirst()
                .orElseThrow();
        SchedulingMachineCandidateInput baselineCandidate =
                new SchedulingMachineCandidateInput(
                        primary.machineId(),
                        primary.priority(),
                        primary.workingTimes(),
                        List.of()
                );
        return new SchedulingOperationInput(
                operation.operationId(),
                operation.machineId(),
                operation.sequence(),
                operation.operationCode(),
                operation.operationName(),
                operation.processingTimeMinutesPerUnit(),
                operation.workingTimes(),
                List.of(),
                List.of(baselineCandidate)
        );
    }

    private DispatchingRuleComparisonResult comparisonResult(
            DispatchingRule rule,
            ScheduleCalculation calculation
    ) {
        LinkedHashMap<String, Boolean> sequence = new LinkedHashMap<>();
        calculation.plan().tasks().forEach(task ->
                sequence.putIfAbsent(task.orderNumber(), Boolean.TRUE));
        ScheduleKpis kpis = calculation.kpis();
        return new DispatchingRuleComparisonResult(
                rule,
                kpis.totalTardinessMinutes(),
                kpis.delayedOrderCount(),
                kpis.makespanMinutes(),
                kpis.machineUtilizationPercent(),
                List.copyOf(sequence.keySet()),
                calculation.plan().tasks()
        );
    }

    private int ruleOrder(DispatchingRule rule) {
        return switch (rule) {
            case EXPLICIT_PRIORITY -> 0;
            case EDD -> 1;
            case SPT -> 2;
        };
    }

    private List<ProductionOrder> findPlanningOrders(
            List<Long> productionOrderIds
    ) {
        if (productionOrderIds == null) {
            return distinctOrders(
                    productionOrderRepository
                            .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                                    ProductionOrderStatus.CONFIRMED
                            )
            );
        }
        if (productionOrderIds.isEmpty()
                || productionOrderIds.stream().anyMatch(
                        id -> id == null || id < 1
                )) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "계획 범위에는 1개 이상의 올바른 생산오더 ID가 필요합니다."
            );
        }
        List<Long> scope = productionOrderIds.stream()
                .distinct()
                .sorted()
                .toList();
        List<ProductionOrder> orders = distinctOrders(
                productionOrderRepository.findAllInScope(
                        scope,
                        ProductionOrderStatus.CONFIRMED
                )
        );
        if (orders.size() != scope.size()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "계획 범위의 생산오더가 없거나 CONFIRMED 상태가 아닙니다."
            );
        }
        return orders;
    }

    @Transactional
    public ScheduleRun reschedule(
            long sourceScheduleRunId,
            UUID executionKey,
            OffsetDateTime frozenAt,
            DispatchingRule requestedRule
    ) {
        if (sourceScheduleRunId < 1
                || executionKey == null
                || frozenAt == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        ScheduleRun existing = scheduleRunRepository
                .findByExecutionKey(executionKey)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        ScheduleRun source = scheduleRunRepository
                .findById(sourceScheduleRunId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.SCHEDULE_RUN_NOT_FOUND
                ));
        if (frozenAt.isBefore(source.planningStart())) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "동결 기준시각은 원본 계획 시작시각보다 이전일 수 없습니다."
            );
        }
        DispatchingRule dispatchingRule = requestedRule == null
                ? source.dispatchingRule()
                : requestedRule;
        ReschedulingPreparation preparation =
                prepareRescheduling(source, frozenAt);
        SchedulingContext context = createContext(
                preparation.planningOrders(),
                source.planningStart(),
                preparation.frozenResources().machines()
        );
        ScheduleCalculation calculation = calculateSchedule(
                source.planningStart(),
                dispatchingRule,
                context,
                preparation.frozenResources().seed()
        );
        ScheduleRun scheduleRun = ScheduleRun.createRescheduled(
                executionKey,
                calculation.plan(),
                OffsetDateTime.now(),
                dispatchingRule,
                calculation.kpis(),
                source,
                frozenAt
        );
        ScheduleReferences references = addFrozenReferences(
                context,
                preparation.frozenOperations()
        );
        addScheduledOperations(
                scheduleRun,
                calculation.plan().tasks(),
                references.ordersById(),
                references.operationsById(),
                references.machinesById()
        );
        for (ProductionOrder order
                : preparation.newlyConfirmedOrders()) {
            order.markScheduled();
        }
        return saveScheduleRun(scheduleRun);
    }

    @Transactional(readOnly = true)
    public ScheduleRun getById(long scheduleRunId) {
        return scheduleRunRepository.findById(scheduleRunId)
                .orElseThrow(() ->
                        new ApplicationException(
                                ErrorCode.SCHEDULE_RUN_NOT_FOUND
                        ));
    }

    @Transactional(readOnly = true)
    public ScheduleRun getLatest() {
        return scheduleRunRepository
                .findTopByOrderByCreatedAtDescIdDesc()
                .orElseThrow(() ->
                        new ApplicationException(
                                ErrorCode.SCHEDULE_RUN_NOT_FOUND
                        ));
    }

    private ReschedulingPreparation prepareRescheduling(
            ScheduleRun source,
            OffsetDateTime frozenAt
    ) {
        List<ScheduledOperation> sourceOperations =
                source.scheduledOperations();
        List<ScheduledOperation> frozenOperations = sourceOperations.stream()
                .filter(operation -> isFrozen(operation, frozenAt))
                .toList();
        List<PlanningOrder> planningOrders = new ArrayList<>();
        Map<Long, OffsetDateTime> orderAvailableAt = new HashMap<>();
        Set<Long> sourceOrderIds = addSourcePlanningOrders(
                groupByOrder(sourceOperations),
                frozenAt,
                planningOrders,
                orderAvailableAt
        );
        List<ProductionOrder> newlyConfirmedOrders =
                findNewlyConfirmedOrders(sourceOrderIds);
        for (ProductionOrder order : newlyConfirmedOrders) {
            planningOrders.add(PlanningOrder.all(order));
            orderAvailableAt.put(order.id(), frozenAt);
        }
        FrozenResources frozenResources = createFrozenResources(
                frozenOperations,
                orderAvailableAt
        );
        return new ReschedulingPreparation(
                List.copyOf(planningOrders),
                frozenOperations,
                newlyConfirmedOrders,
                frozenResources
        );
    }

    private Map<Long, List<ScheduledOperation>> groupByOrder(
            List<ScheduledOperation> operations
    ) {
        Map<Long, List<ScheduledOperation>> grouped =
                new LinkedHashMap<>();
        for (ScheduledOperation operation : operations) {
            grouped.computeIfAbsent(
                    operation.productionOrder().id(),
                    ignored -> new ArrayList<>()
            ).add(operation);
        }
        return grouped;
    }

    private Set<Long> addSourcePlanningOrders(
            Map<Long, List<ScheduledOperation>> sourceByOrder,
            OffsetDateTime frozenAt,
            List<PlanningOrder> planningOrders,
            Map<Long, OffsetDateTime> orderAvailableAt
    ) {
        Set<Long> sourceOrderIds = new HashSet<>();
        for (List<ScheduledOperation> operations
                : sourceByOrder.values()) {
            ProductionOrder order =
                    operations.getFirst().productionOrder();
            sourceOrderIds.add(order.id());
            if (order.status() == ProductionOrderStatus.CANCELLED) {
                continue;
            }
            Set<Long> futureOperationIds = new HashSet<>();
            OffsetDateTime availableAt = frozenAt;
            for (ScheduledOperation operation : operations) {
                if (isFrozen(operation, frozenAt)) {
                    availableAt = max(availableAt, operation.endAt());
                } else {
                    futureOperationIds.add(operation.operation().id());
                }
            }
            if (!futureOperationIds.isEmpty()) {
                planningOrders.add(new PlanningOrder(
                        order,
                        futureOperationIds
                ));
                orderAvailableAt.put(order.id(), availableAt);
            }
        }
        return sourceOrderIds;
    }

    private List<ProductionOrder> findNewlyConfirmedOrders(
            Set<Long> sourceOrderIds
    ) {
        return distinctOrders(
                productionOrderRepository
                        .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                                ProductionOrderStatus.CONFIRMED
                        )
        ).stream()
                .filter(order -> !sourceOrderIds.contains(order.id()))
                .toList();
    }

    private FrozenResources createFrozenResources(
            List<ScheduledOperation> frozenOperations,
            Map<Long, OffsetDateTime> orderAvailableAt
    ) {
        Map<Long, Machine> machines = new HashMap<>();
        Map<Long, OffsetDateTime> machineAvailableAt = new HashMap<>();
        Map<Long, ScheduledOperation> lastOperationByMachine =
                new HashMap<>();
        List<ScheduledTask> tasks =
                new ArrayList<>(frozenOperations.size());
        for (ScheduledOperation operation : frozenOperations) {
            long machineId = operation.machine().id();
            machines.put(machineId, operation.machine());
            tasks.add(toScheduledTask(operation));
            machineAvailableAt.merge(
                    machineId,
                    operation.endAt(),
                    this::max
            );
            lastOperationByMachine.merge(
                    machineId,
                    operation,
                    (left, right) -> right.endAt().isAfter(left.endAt())
                            ? right
                            : left
            );
        }
        Map<Long, Long> lastProductByMachine = new HashMap<>();
        for (Map.Entry<Long, ScheduledOperation> entry
                : lastOperationByMachine.entrySet()) {
            lastProductByMachine.put(
                    entry.getKey(),
                    entry.getValue()
                            .productionOrder()
                            .routing()
                            .product()
                            .id()
            );
        }
        return new FrozenResources(
                Map.copyOf(machines),
                new FrozenScheduleSeed(
                        tasks,
                        machineAvailableAt,
                        lastProductByMachine,
                        orderAvailableAt
                )
        );
    }

    private ScheduleCalculation calculateSchedule(
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule,
            SchedulingContext context,
            FrozenScheduleSeed seed
    ) {
        try {
            ForwardScheduler scheduler = new ForwardScheduler(
                    dispatchingRule.priorityRule()
            );
            SchedulingPlan plan = scheduler.schedule(
                    planningStart,
                    context.inputs(),
                    context.changeoverInputs(),
                    seed
            );
            ScheduleKpis kpis = kpiCalculator.calculate(
                    plan,
                    context.inputs(),
                    context.capacityCandidates()
            );
            return new ScheduleCalculation(plan, kpis);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    private ScheduleReferences addFrozenReferences(
            SchedulingContext context,
            List<ScheduledOperation> frozenOperations
    ) {
        Map<Long, ProductionOrder> ordersById =
                new HashMap<>(context.ordersById());
        Map<Long, Operation> operationsById =
                new HashMap<>(context.operationsById());
        Map<Long, Machine> machinesById =
                new HashMap<>(context.machinesById());
        for (ScheduledOperation operation : frozenOperations) {
            ordersById.put(
                    operation.productionOrder().id(),
                    operation.productionOrder()
            );
            operationsById.put(
                    operation.operation().id(),
                    operation.operation()
            );
            machinesById.put(
                    operation.machine().id(),
                    operation.machine()
            );
        }
        return new ScheduleReferences(
                Map.copyOf(ordersById),
                Map.copyOf(operationsById),
                Map.copyOf(machinesById)
        );
    }

    private ScheduleRun saveScheduleRun(ScheduleRun scheduleRun) {
        try {
            return scheduleRunRepository.saveAndFlush(scheduleRun);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(
                    exception,
                    "uk_schedule_run_execution_key"
            )) {
                throw new ApplicationException(
                        ErrorCode.SCHEDULE_EXECUTION_DUPLICATED,
                        ErrorCode.SCHEDULE_EXECUTION_DUPLICATED
                                .defaultMessage(),
                        exception
                );
            }
            throw exception;
        }
    }

    private SchedulingContext createContext(
            List<ProductionOrder> orders,
            OffsetDateTime planningStart
    ) {
        return createContext(
                orders.stream()
                        .map(PlanningOrder::all)
                        .toList(),
                planningStart,
                Map.of()
        );
    }

    private SchedulingContext createContext(
            List<PlanningOrder> planningOrders,
            OffsetDateTime planningStart,
            Map<Long, Machine> additionalMachines
    ) {
        Set<Long> machineIds = new HashSet<>();
        Map<Long, ProductionOrder> ordersById = new HashMap<>();
        Map<Long, Operation> operationsById = new HashMap<>();
        Map<Long, Machine> machinesById =
                new HashMap<>(additionalMachines);
        machineIds.addAll(additionalMachines.keySet());

        for (PlanningOrder planningOrder : planningOrders) {
            ProductionOrder order = planningOrder.order();
            ordersById.put(order.id(), order);
            for (Operation operation : selectedOperations(planningOrder)) {
                int availableCandidateCount = 0;
                for (OperationMachineCandidate candidate
                        : operation.machineCandidates()) {
                    Machine candidateMachine = candidate.machine();
                    if (candidateMachine.status()
                            != MachineStatus.AVAILABLE) {
                        continue;
                    }
                    availableCandidateCount++;
                    machineIds.add(candidateMachine.id());
                    machinesById.put(
                            candidateMachine.id(),
                            candidateMachine
                    );
                }
                if (availableCandidateCount == 0) {
                    throw new ApplicationException(
                            ErrorCode.MACHINE_UNAVAILABLE_FOR_SCHEDULING
                    );
                }
                operationsById.put(operation.id(), operation);
            }
        }

        Map<Long, List<WeeklyWorkingTime>> workingTimesByMachine =
                loadWorkingTimes(machineIds);
        List<SchedulingChangeoverInput> changeoverInputs =
                loadChangeoverInputs(machineIds);
        Map<Long, List<UnavailableInterval>> unavailableByMachine =
                loadUnavailableIntervals(machineIds, planningStart);
        for (Long machineId : additionalMachines.keySet()) {
            if (workingTimesByMachine.getOrDefault(
                    machineId,
                    List.of()
            ).isEmpty()) {
                throw new ApplicationException(
                        ErrorCode.WORKING_CALENDAR_REQUIRED,
                        "동결 작업 설비의 근무시간이 필요합니다."
                );
            }
        }
        List<SchedulingMachineCandidateInput> capacityCandidates =
                new ArrayList<>();
        for (Long machineId : machineIds) {
            List<WeeklyWorkingTime> workingTimes =
                    workingTimesByMachine.getOrDefault(
                            machineId,
                            List.of()
                    );
            if (!workingTimes.isEmpty()) {
                capacityCandidates.add(
                        new SchedulingMachineCandidateInput(
                                machineId,
                                1,
                                workingTimes,
                                unavailableByMachine.getOrDefault(
                                        machineId,
                                        List.of()
                                )
                        )
                );
            }
        }
        List<SchedulingOrderInput> inputs =
                new ArrayList<>(planningOrders.size());
        for (PlanningOrder planningOrder : planningOrders) {
            ProductionOrder order = planningOrder.order();
            List<SchedulingOperationInput> operationInputs =
                    new ArrayList<>();
            for (Operation operation : selectedOperations(planningOrder)) {
                List<SchedulingMachineCandidateInput> candidateInputs =
                        new ArrayList<>();
                for (OperationMachineCandidate candidate
                        : operation.machineCandidates()) {
                    Machine candidateMachine = candidate.machine();
                    if (candidateMachine.status()
                            != MachineStatus.AVAILABLE) {
                        continue;
                    }
                    List<WeeklyWorkingTime> candidateWorkingTimes =
                            workingTimesByMachine.getOrDefault(
                                    candidateMachine.id(),
                                    List.of()
                            );
                    if (candidateWorkingTimes.isEmpty()) {
                        continue;
                    }
                    candidateInputs.add(
                            new SchedulingMachineCandidateInput(
                                    candidateMachine.id(),
                                    candidate.priority(),
                                    candidateWorkingTimes,
                                    unavailableByMachine.getOrDefault(
                                            candidateMachine.id(),
                                            List.of()
                                    )
                            )
                    );
                }
                if (candidateInputs.isEmpty()) {
                    throw new ApplicationException(
                            ErrorCode.WORKING_CALENDAR_REQUIRED,
                            "Operation %s의 후보 설비 근무시간이 필요합니다."
                                    .formatted(operation.code())
                    );
                }
                List<WeeklyWorkingTime> primaryWorkingTimes =
                        workingTimesByMachine.getOrDefault(
                                operation.machine().id(),
                                List.of()
                        );
                operationInputs.add(new SchedulingOperationInput(
                        operation.id(),
                        operation.machine().id(),
                        operation.sequence(),
                        operation.code(),
                        operation.name(),
                        operation.processingTimeMinutes(),
                        primaryWorkingTimes,
                        unavailableByMachine.getOrDefault(
                                operation.machine().id(),
                                List.of()
                        ),
                        candidateInputs
                ));
            }
            inputs.add(new SchedulingOrderInput(
                    order.id(),
                    order.orderNumber(),
                    order.routing().product().id(),
                    order.quantity(),
                    order.releaseAt(),
                    order.dueAt(),
                    order.priority(),
                    operationInputs
            ));
        }
        return new SchedulingContext(
                List.copyOf(inputs),
                changeoverInputs,
                List.copyOf(capacityCandidates),
                Map.copyOf(ordersById),
                Map.copyOf(operationsById),
                Map.copyOf(machinesById)
        );
    }

    private Map<Long, List<UnavailableInterval>> loadUnavailableIntervals(
            Set<Long> machineIds,
            OffsetDateTime planningStart
    ) {
        if (machineIds.isEmpty()) {
            return Map.of();
        }
        List<MachineMaintenance> maintenances = maintenanceRepository
                .findAllByMachine_IdInAndActiveTrueAndEndAtGreaterThanOrderByStartAtAsc(
                        machineIds,
                        planningStart
                );
        Map<Long, List<UnavailableInterval>> unavailableByMachine =
                new HashMap<>();
        for (MachineMaintenance maintenance : maintenances) {
            unavailableByMachine
                    .computeIfAbsent(
                            maintenance.machine().id(),
                            ignored -> new ArrayList<>()
                    )
                    .add(maintenance.toUnavailableInterval());
        }
        return unavailableByMachine;
    }

    private List<SchedulingChangeoverInput> loadChangeoverInputs(
            Set<Long> machineIds
    ) {
        if (machineIds.isEmpty()) {
            return List.of();
        }
        List<ChangeoverTime> changeoverTimes = changeoverTimeRepository
                .findAllByMachine_IdInAndActiveTrue(machineIds);
        List<SchedulingChangeoverInput> inputs =
                new ArrayList<>(changeoverTimes.size());
        for (ChangeoverTime changeoverTime : changeoverTimes) {
            inputs.add(new SchedulingChangeoverInput(
                    changeoverTime.machine().id(),
                    changeoverTime.fromProduct().id(),
                    changeoverTime.toProduct().id(),
                    changeoverTime.changeoverMinutes()
            ));
        }
        return List.copyOf(inputs);
    }

    private List<ProductionOrder> distinctOrders(
            List<ProductionOrder> queriedOrders
    ) {
        Map<Long, ProductionOrder> ordersById = new LinkedHashMap<>();
        for (ProductionOrder order : queriedOrders) {
            ordersById.putIfAbsent(order.id(), order);
        }
        return List.copyOf(ordersById.values());
    }

    private List<Operation> selectedOperations(
            PlanningOrder planningOrder
    ) {
        return distinctOperations(
                planningOrder.order().routing().operations()
        ).stream()
                .filter(operation ->
                        planningOrder.operationIds().contains(operation.id()))
                .toList();
    }

    private List<Operation> distinctOperations(
            List<Operation> queriedOperations
    ) {
        Map<Long, Operation> operationsById = new LinkedHashMap<>();
        for (Operation operation : queriedOperations) {
            operationsById.putIfAbsent(operation.id(), operation);
        }
        return List.copyOf(operationsById.values());
    }

    private Map<Long, List<WeeklyWorkingTime>> loadWorkingTimes(
            Set<Long> machineIds
    ) {
        if (machineIds.isEmpty()) {
            return Map.of();
        }
        List<WorkingCalendar> calendars = workingCalendarRepository
                .findAllByMachine_IdInAndActiveTrue(machineIds);
        Map<Long, List<WeeklyWorkingTime>> workingTimesByMachine =
                new HashMap<>();
        for (WorkingCalendar calendar : calendars) {
            workingTimesByMachine
                    .computeIfAbsent(
                            calendar.machine().id(),
                            ignored -> new ArrayList<>()
                    )
                    .add(calendar.toWeeklyWorkingTime());
        }
        return workingTimesByMachine;
    }

    private void addScheduledOperations(
            ScheduleRun scheduleRun,
            List<ScheduledTask> tasks,
            Map<Long, ProductionOrder> ordersById,
            Map<Long, Operation> operationsById,
            Map<Long, Machine> machinesById
    ) {
        for (ScheduledTask task : tasks) {
            ProductionOrder order = ordersById.get(task.orderId());
            Operation operation = operationsById.get(task.operationId());
            Machine machine = machinesById.get(task.machineId());
            if (order == null || operation == null || machine == null) {
                throw new IllegalStateException(
                        "스케줄 저장에 필요한 오더·공정·설비를 찾을 수 없습니다."
                );
            }
            scheduleRun.addScheduledOperation(
                    order,
                    operation,
                    machine,
                    task
            );
        }
    }

    private ScheduledTask toScheduledTask(
            ScheduledOperation operation
    ) {
        ProductionOrder order = operation.productionOrder();
        Operation routingOperation = operation.operation();
        return new ScheduledTask(
                order.id(),
                order.orderNumber(),
                routingOperation.id(),
                operation.machine().id(),
                operation.sequence(),
                routingOperation.code(),
                routingOperation.name(),
                operation.changeoverStartAt(),
                operation.changeoverMinutes(),
                operation.startAt(),
                operation.endAt(),
                operation.workingMinutes(),
                order.dueAt(),
                operation.delayed()
        );
    }

    private boolean isFrozen(
            ScheduledOperation operation,
            OffsetDateTime frozenAt
    ) {
        OffsetDateTime effectiveStart =
                operation.changeoverStartAt() == null
                        ? operation.startAt()
                        : operation.changeoverStartAt();
        return effectiveStart.isBefore(frozenAt);
    }

    private OffsetDateTime max(
            OffsetDateTime left,
            OffsetDateTime right
    ) {
        return right.isAfter(left) ? right : left;
    }

    private boolean hasConstraint(
            Throwable exception,
            String constraintName
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null
                    && cause.getMessage().contains(constraintName)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private record SchedulingContext(
            List<SchedulingOrderInput> inputs,
            List<SchedulingChangeoverInput> changeoverInputs,
            List<SchedulingMachineCandidateInput> capacityCandidates,
            Map<Long, ProductionOrder> ordersById,
            Map<Long, Operation> operationsById,
            Map<Long, Machine> machinesById
    ) {
    }

    private record PlanningOrder(
            ProductionOrder order,
            Set<Long> operationIds
    ) {

        private PlanningOrder {
            operationIds = Set.copyOf(operationIds);
        }

        private static PlanningOrder all(ProductionOrder order) {
            return new PlanningOrder(
                    order,
                    order.routing().operations().stream()
                            .map(Operation::id)
                            .collect(java.util.stream.Collectors.toSet())
            );
        }
    }

    private record ReschedulingPreparation(
            List<PlanningOrder> planningOrders,
            List<ScheduledOperation> frozenOperations,
            List<ProductionOrder> newlyConfirmedOrders,
            FrozenResources frozenResources
    ) {
    }

    private record FrozenResources(
            Map<Long, Machine> machines,
            FrozenScheduleSeed seed
    ) {
    }

    private record ScheduleCalculation(
            SchedulingPlan plan,
            ScheduleKpis kpis
    ) {
    }

    private record ScheduleReferences(
            Map<Long, ProductionOrder> ordersById,
            Map<Long, Operation> operationsById,
            Map<Long, Machine> machinesById
    ) {
    }
}
