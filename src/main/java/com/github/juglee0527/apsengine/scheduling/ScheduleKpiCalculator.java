package com.github.juglee0527.apsengine.scheduling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.juglee0527.apsengine.capacity.WorkingTimeCalculator;

public class ScheduleKpiCalculator {

    private final WorkingTimeCalculator workingTimeCalculator =
            new WorkingTimeCalculator();

    public ScheduleKpis calculate(
            SchedulingPlan plan,
            List<SchedulingOrderInput> orders
    ) {
        List<SchedulingMachineCandidateInput> candidates = orders.stream()
                .flatMap(order -> order.operations().stream())
                .flatMap(operation ->
                        operation.machineCandidates().stream())
                .toList();
        return calculate(plan, orders, candidates);
    }

    ScheduleKpis calculate(
            SchedulingPlan plan,
            List<SchedulingOrderInput> orders,
            List<SchedulingMachineCandidateInput> capacityCandidates
    ) {
        if (plan.tasks().isEmpty()) {
            return ScheduleKpis.empty();
        }

        Map<Long, OffsetDateTime> dueAtByOrder = new HashMap<>();
        Map<Long, OffsetDateTime> completionByOrder = new HashMap<>();
        Map<Long, SchedulingMachineCandidateInput> candidatesByMachine =
                new HashMap<>();
        for (SchedulingOrderInput order : orders) {
            dueAtByOrder.put(order.orderId(), order.dueAt());
        }
        for (SchedulingMachineCandidateInput candidate
                : capacityCandidates) {
            candidatesByMachine.putIfAbsent(
                    candidate.machineId(),
                    candidate
            );
        }

        Map<Long, Long> loadByMachine = new HashMap<>();
        for (ScheduledTask task : plan.tasks()) {
            dueAtByOrder.putIfAbsent(task.orderId(), task.dueAt());
            completionByOrder.merge(
                    task.orderId(),
                    task.endAt(),
                    (left, right) -> right.isAfter(left) ? right : left
            );
            long taskLoad = Math.addExact(
                    task.workingMinutes(),
                    task.changeoverMinutes()
            );
            loadByMachine.merge(
                    task.machineId(),
                    taskLoad,
                    Math::addExact
            );
        }

        long totalTardinessMinutes = 0;
        int delayedOrderCount = 0;
        for (Map.Entry<Long, OffsetDateTime> completion
                : completionByOrder.entrySet()) {
            OffsetDateTime dueAt = dueAtByOrder.get(completion.getKey());
            if (completion.getValue().isAfter(dueAt)) {
                delayedOrderCount++;
                totalTardinessMinutes = Math.addExact(
                        totalTardinessMinutes,
                        Duration.between(
                                dueAt,
                                completion.getValue()
                        ).toMinutes()
                );
            }
        }

        long makespanMinutes = Duration.between(
                plan.planningStart(),
                plan.schedulingEnd()
        ).toMinutes();
        long totalLoadMinutes = 0;
        long totalAvailableMinutes = 0;
        for (Map.Entry<Long, Long> load : loadByMachine.entrySet()) {
            SchedulingMachineCandidateInput candidate =
                    candidatesByMachine.get(load.getKey());
            if (candidate == null) {
                throw new IllegalStateException(
                        "선택 설비의 가용시간 스냅샷을 찾을 수 없습니다."
                );
            }
            totalLoadMinutes = Math.addExact(
                    totalLoadMinutes,
                    load.getValue()
            );
            totalAvailableMinutes = Math.addExact(
                    totalAvailableMinutes,
                    workingTimeCalculator.availableMinutes(
                            candidate.workingTimes(),
                            candidate.unavailableIntervals(),
                            plan.planningStart(),
                            plan.schedulingEnd()
                    )
            );
        }

        BigDecimal utilization = totalAvailableMinutes == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalLoadMinutes)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(totalAvailableMinutes),
                                2,
                                RoundingMode.HALF_UP
                        );
        return new ScheduleKpis(
                totalTardinessMinutes,
                delayedOrderCount,
                makespanMinutes,
                utilization
        );
    }
}
