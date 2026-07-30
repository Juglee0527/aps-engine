package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
import com.github.juglee0527.apsengine.machine.MachineStatus;
import com.github.juglee0527.apsengine.order.ProductionOrder;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderStatus;
import com.github.juglee0527.apsengine.product.routing.Operation;

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
    private final ForwardScheduler forwardScheduler;

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
        this.forwardScheduler = new ForwardScheduler();
    }

    @Transactional
    public ScheduleRun execute(
            UUID executionKey,
            OffsetDateTime planningStart
    ) {
        if (executionKey == null || planningStart == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        ScheduleRun existing = scheduleRunRepository
                .findByExecutionKey(executionKey)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        List<ProductionOrder> orders = distinctOrders(
                productionOrderRepository
                        .findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
                                ProductionOrderStatus.CONFIRMED
                        )
        );
        if (orders.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.CONFIRMED_PRODUCTION_ORDER_REQUIRED
            );
        }

        SchedulingContext context = createContext(orders, planningStart);
        SchedulingPlan plan;
        try {
            plan = forwardScheduler.schedule(
                    planningStart,
                    context.inputs(),
                    context.changeoverInputs()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }

        ScheduleRun scheduleRun = ScheduleRun.create(
                executionKey,
                plan,
                OffsetDateTime.now()
        );
        for (ScheduledTask task : plan.tasks()) {
            ProductionOrder order = context.ordersById()
                    .get(task.orderId());
            Operation operation = context.operationsById()
                    .get(task.operationId());
            scheduleRun.addScheduledOperation(
                    order,
                    operation,
                    operation.machine(),
                    task
            );
        }
        for (ProductionOrder order : orders) {
            order.markScheduled();
        }

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

    private SchedulingContext createContext(
            List<ProductionOrder> orders,
            OffsetDateTime planningStart
    ) {
        Set<Long> machineIds = new HashSet<>();
        Map<Long, ProductionOrder> ordersById = new HashMap<>();
        Map<Long, Operation> operationsById = new HashMap<>();

        for (ProductionOrder order : orders) {
            ordersById.put(order.id(), order);
            for (Operation operation : distinctOperations(
                    order.routing().operations()
            )) {
                if (operation.machine().status()
                        != MachineStatus.AVAILABLE) {
                    throw new ApplicationException(
                            ErrorCode.MACHINE_UNAVAILABLE_FOR_SCHEDULING
                    );
                }
                machineIds.add(operation.machine().id());
                operationsById.put(operation.id(), operation);
            }
        }

        Map<Long, List<WeeklyWorkingTime>> workingTimesByMachine =
                loadWorkingTimes(machineIds);
        List<SchedulingChangeoverInput> changeoverInputs =
                loadChangeoverInputs(machineIds);
        Map<Long, List<UnavailableInterval>> unavailableByMachine =
                loadUnavailableIntervals(machineIds, planningStart);
        List<SchedulingOrderInput> inputs =
                new ArrayList<>(orders.size());
        for (ProductionOrder order : orders) {
            List<SchedulingOperationInput> operationInputs =
                    new ArrayList<>();
            for (Operation operation : distinctOperations(
                    order.routing().operations()
            )) {
                List<WeeklyWorkingTime> workingTimes =
                        workingTimesByMachine.getOrDefault(
                                operation.machine().id(),
                                List.of()
                        );
                if (workingTimes.isEmpty()) {
                    throw new ApplicationException(
                            ErrorCode.WORKING_CALENDAR_REQUIRED,
                            "설비 %s의 근무시간이 필요합니다."
                                    .formatted(operation.machine().code())
                    );
                }
                operationInputs.add(new SchedulingOperationInput(
                        operation.id(),
                        operation.machine().id(),
                        operation.sequence(),
                        operation.code(),
                        operation.name(),
                        operation.processingTimeMinutes(),
                        workingTimes,
                        unavailableByMachine.getOrDefault(
                                operation.machine().id(),
                                List.of()
                        )
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
                Map.copyOf(ordersById),
                Map.copyOf(operationsById)
        );
    }

    private Map<Long, List<UnavailableInterval>> loadUnavailableIntervals(
            Set<Long> machineIds,
            OffsetDateTime planningStart
    ) {
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
            Map<Long, ProductionOrder> ordersById,
            Map<Long, Operation> operationsById
    ) {
    }
}
