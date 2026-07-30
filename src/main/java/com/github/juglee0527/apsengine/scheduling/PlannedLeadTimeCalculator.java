package com.github.juglee0527.apsengine.scheduling;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PlannedLeadTimeCalculator {

    List<PlannedLeadTime> calculate(
            List<LeadTimeOperationInput> operationInputs
    ) {
        if (operationInputs == null) {
            throw new IllegalArgumentException(
                    "Lead Time 계산 입력은 null일 수 없습니다."
            );
        }
        Map<Long, OrderAccumulator> accumulators =
                new LinkedHashMap<>();
        for (LeadTimeOperationInput input : operationInputs) {
            if (input == null) {
                throw new IllegalArgumentException(
                        "Lead Time 공정 입력은 null일 수 없습니다."
                );
            }
            accumulators.computeIfAbsent(
                    input.productionOrderId(),
                    ignored -> new OrderAccumulator(input)
            ).add(input);
        }

        List<PlannedLeadTime> results =
                new ArrayList<>(accumulators.size());
        for (OrderAccumulator accumulator : accumulators.values()) {
            results.add(accumulator.toResult());
        }
        results.sort(Comparator.comparingLong(
                PlannedLeadTime::productionOrderId
        ));
        return List.copyOf(results);
    }

    private static final class OrderAccumulator {

        private final long productionOrderId;
        private final String orderNumber;
        private final long productId;
        private final String productCode;
        private final OffsetDateTime releaseAt;
        private OffsetDateTime completionAt;
        private long processingMinutes;
        private long changeoverMinutes;
        private int operationCount;

        private OrderAccumulator(LeadTimeOperationInput first) {
            this.productionOrderId = first.productionOrderId();
            this.orderNumber = first.orderNumber();
            this.productId = first.productId();
            this.productCode = first.productCode();
            this.releaseAt = first.releaseAt();
            this.completionAt = first.endAt();
        }

        private void add(LeadTimeOperationInput input) {
            validateSameOrder(input);
            if (input.endAt().isAfter(completionAt)) {
                completionAt = input.endAt();
            }
            processingMinutes = Math.addExact(
                    processingMinutes,
                    input.processingMinutes()
            );
            changeoverMinutes = Math.addExact(
                    changeoverMinutes,
                    input.changeoverMinutes()
            );
            operationCount = Math.addExact(operationCount, 1);
        }

        private PlannedLeadTime toResult() {
            long plannedLeadTimeMinutes = Duration.between(
                    releaseAt,
                    completionAt
            ).toMinutes();
            long occupiedMinutes = Math.addExact(
                    processingMinutes,
                    changeoverMinutes
            );
            long waitingMinutes = Math.subtractExact(
                    plannedLeadTimeMinutes,
                    occupiedMinutes
            );
            if (waitingMinutes < 0) {
                throw new IllegalStateException(
                        "계획 Lead Time보다 가공시간과 Changeover Time 합계가 큽니다."
                );
            }
            return new PlannedLeadTime(
                    productionOrderId,
                    orderNumber,
                    productId,
                    productCode,
                    releaseAt,
                    completionAt,
                    plannedLeadTimeMinutes,
                    processingMinutes,
                    changeoverMinutes,
                    waitingMinutes,
                    operationCount
            );
        }

        private void validateSameOrder(LeadTimeOperationInput input) {
            if (!orderNumber.equals(input.orderNumber())
                    || productId != input.productId()
                    || !productCode.equals(input.productCode())
                    || !releaseAt.isEqual(input.releaseAt())) {
                throw new IllegalArgumentException(
                        "같은 생산오더의 Lead Time 입력 정보가 일치하지 않습니다."
                );
            }
        }
    }
}
