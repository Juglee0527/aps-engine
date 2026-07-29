package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.juglee0527.apsengine.capacity.WorkingAllocation;
import com.github.juglee0527.apsengine.capacity.WorkingTimeCalculator;

public class ForwardScheduler {

    private static final Comparator<SchedulingOperationInput>
            OPERATION_SEQUENCE =
            Comparator.comparingInt(SchedulingOperationInput::sequence);

    private final WorkingTimeCalculator workingTimeCalculator;
    private final SchedulingPriorityRule priorityRule;

    public ForwardScheduler() {
        this(new ExplicitPriorityRule());
    }

    public ForwardScheduler(SchedulingPriorityRule priorityRule) {
        if (priorityRule == null) {
            throw new IllegalArgumentException(
                    "생산오더 우선순위 규칙은 필수입니다."
            );
        }
        this.workingTimeCalculator = new WorkingTimeCalculator();
        this.priorityRule = priorityRule;
    }

    public SchedulingPlan schedule(
            OffsetDateTime planningStart,
            List<SchedulingOrderInput> orders
    ) {
        if (planningStart == null) {
            throw new IllegalArgumentException(
                    "계획 시작시각은 필수입니다."
            );
        }
        if (orders == null) {
            throw new IllegalArgumentException(
                    "생산오더 목록은 null일 수 없습니다."
            );
        }

        List<SchedulingOrderInput> orderedOrders =
                new ArrayList<>(orders);
        orderedOrders.sort(priorityRule);

        List<ScheduledTask> tasks = new ArrayList<>();
        Map<Long, OffsetDateTime> machineAvailableAt =
                new HashMap<>();
        OffsetDateTime schedulingEnd = planningStart;

        for (SchedulingOrderInput order : orderedOrders) {
            OffsetDateTime normalizedReleaseAt =
                    order.releaseAt().withOffsetSameInstant(
                            planningStart.getOffset()
                    );
            OffsetDateTime precedingOperationEnd =
                    max(planningStart, normalizedReleaseAt);
            List<SchedulingOperationInput> operations =
                    new ArrayList<>(order.operations());
            operations.sort(OPERATION_SEQUENCE);

            for (SchedulingOperationInput operation : operations) {
                OffsetDateTime earliestStart = max(
                        precedingOperationEnd,
                        machineAvailableAt.get(operation.machineId())
                );
                long requiredMinutes = requiredMinutes(order, operation);
                WorkingAllocation allocation =
                        workingTimeCalculator.allocate(
                                operation.workingTimes(),
                                earliestStart,
                                requiredMinutes
                        );
                boolean delayed =
                        allocation.endAt().isAfter(order.dueAt());
                tasks.add(new ScheduledTask(
                        order.orderId(),
                        order.orderNumber(),
                        operation.operationId(),
                        operation.machineId(),
                        operation.sequence(),
                        operation.operationCode(),
                        operation.operationName(),
                        allocation.startAt(),
                        allocation.endAt(),
                        allocation.workingMinutes(),
                        order.dueAt(),
                        delayed
                ));
                precedingOperationEnd = allocation.endAt();
                machineAvailableAt.put(
                        operation.machineId(),
                        allocation.endAt()
                );
                schedulingEnd = max(
                        schedulingEnd,
                        allocation.endAt()
                );
            }
        }
        return new SchedulingPlan(
                planningStart,
                schedulingEnd,
                tasks
        );
    }

    private long requiredMinutes(
            SchedulingOrderInput order,
            SchedulingOperationInput operation
    ) {
        try {
            return Math.multiplyExact(
                    order.quantity(),
                    operation.processingTimeMinutesPerUnit()
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "생산수량과 단위 처리시간의 곱이 허용 범위를 초과합니다.",
                    exception
            );
        }
    }

    private OffsetDateTime max(
            OffsetDateTime left,
            OffsetDateTime right
    ) {
        if (right == null || !right.isAfter(left)) {
            return left;
        }
        return right.withOffsetSameInstant(left.getOffset());
    }
}
