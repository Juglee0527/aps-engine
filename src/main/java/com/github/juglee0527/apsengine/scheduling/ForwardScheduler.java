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
    private static final Comparator<CandidateAllocation>
            CANDIDATE_SELECTION =
            Comparator.comparing(
                            (CandidateAllocation candidate) ->
                                    candidate.processingAllocation()
                                    .endAt()
                                    .toInstant()
                    )
                    .thenComparingInt(candidate ->
                            candidate.candidate().priority())
                    .thenComparingLong(candidate ->
                            candidate.candidate().machineId());

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
        return schedule(planningStart, orders, List.of());
    }

    public SchedulingPlan schedule(
            OffsetDateTime planningStart,
            List<SchedulingOrderInput> orders,
            List<SchedulingChangeoverInput> changeoverInputs
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
        ChangeoverTimeLookup changeoverTimeLookup =
                ChangeoverTimeLookup.from(changeoverInputs);

        List<SchedulingOrderInput> orderedOrders =
                new ArrayList<>(orders);
        orderedOrders.sort(priorityRule);

        List<ScheduledTask> tasks = new ArrayList<>();
        Map<Long, OffsetDateTime> machineAvailableAt =
                new HashMap<>();
        Map<Long, Long> lastProductByMachine = new HashMap<>();
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
                long requiredMinutes = requiredMinutes(order, operation);
                CandidateAllocation selected = selectCandidate(
                        order,
                        operation,
                        precedingOperationEnd,
                        requiredMinutes,
                        changeoverTimeLookup,
                        machineAvailableAt,
                        lastProductByMachine
                );
                SchedulingMachineCandidateInput selectedCandidate =
                        selected.candidate();
                WorkingAllocation allocation =
                        selected.processingAllocation();
                boolean delayed =
                        allocation.endAt().isAfter(order.dueAt());
                tasks.add(new ScheduledTask(
                        order.orderId(),
                        order.orderNumber(),
                        operation.operationId(),
                        selectedCandidate.machineId(),
                        operation.sequence(),
                        operation.operationCode(),
                        operation.operationName(),
                        selected.changeoverStartAt(),
                        selected.changeoverMinutes(),
                        allocation.startAt(),
                        allocation.endAt(),
                        allocation.workingMinutes(),
                        order.dueAt(),
                        delayed
                ));
                precedingOperationEnd = allocation.endAt();
                machineAvailableAt.put(
                        selectedCandidate.machineId(),
                        allocation.endAt()
                );
                lastProductByMachine.put(
                        selectedCandidate.machineId(),
                        order.productId()
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

    private CandidateAllocation selectCandidate(
            SchedulingOrderInput order,
            SchedulingOperationInput operation,
            OffsetDateTime precedingOperationEnd,
            long requiredMinutes,
            ChangeoverTimeLookup changeoverTimeLookup,
            Map<Long, OffsetDateTime> machineAvailableAt,
            Map<Long, Long> lastProductByMachine
    ) {
        CandidateAllocation selected = null;
        for (SchedulingMachineCandidateInput candidate
                : operation.machineCandidates()) {
            CandidateAllocation allocation = allocateCandidate(
                    order,
                    candidate,
                    precedingOperationEnd,
                    requiredMinutes,
                    changeoverTimeLookup,
                    machineAvailableAt,
                    lastProductByMachine
            );
            if (selected == null
                    || CANDIDATE_SELECTION.compare(
                            allocation,
                            selected
                    ) < 0) {
                selected = allocation;
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException(
                    "스케줄링할 후보 설비가 없습니다."
            );
        }
        return selected;
    }

    private CandidateAllocation allocateCandidate(
            SchedulingOrderInput order,
            SchedulingMachineCandidateInput candidate,
            OffsetDateTime precedingOperationEnd,
            long requiredMinutes,
            ChangeoverTimeLookup changeoverTimeLookup,
            Map<Long, OffsetDateTime> machineAvailableAt,
            Map<Long, Long> lastProductByMachine
    ) {
        OffsetDateTime earliestStart = max(
                precedingOperationEnd,
                machineAvailableAt.get(candidate.machineId())
        );
        long changeoverMinutes = changeoverMinutes(
                changeoverTimeLookup,
                lastProductByMachine.get(candidate.machineId()),
                order.productId(),
                candidate.machineId()
        );
        OffsetDateTime changeoverStartAt = null;
        if (changeoverMinutes > 0) {
            WorkingAllocation changeoverAllocation =
                    workingTimeCalculator.allocate(
                            candidate.workingTimes(),
                            candidate.unavailableIntervals(),
                            earliestStart,
                            changeoverMinutes
                    );
            changeoverStartAt = changeoverAllocation.startAt();
            earliestStart = changeoverAllocation.endAt();
        }
        WorkingAllocation processingAllocation =
                workingTimeCalculator.allocate(
                        candidate.workingTimes(),
                        candidate.unavailableIntervals(),
                        earliestStart,
                        requiredMinutes
                );
        return new CandidateAllocation(
                candidate,
                changeoverStartAt,
                changeoverMinutes,
                processingAllocation
        );
    }

    private long changeoverMinutes(
            ChangeoverTimeLookup changeoverTimeLookup,
            Long previousProductId,
            long nextProductId,
            long machineId
    ) {
        if (previousProductId == null) {
            return 0;
        }
        return changeoverTimeLookup.minutesFor(
                machineId,
                previousProductId,
                nextProductId
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

    private record CandidateAllocation(
            SchedulingMachineCandidateInput candidate,
            OffsetDateTime changeoverStartAt,
            long changeoverMinutes,
            WorkingAllocation processingAllocation
    ) {
    }
}
