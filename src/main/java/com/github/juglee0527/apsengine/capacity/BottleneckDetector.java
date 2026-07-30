package com.github.juglee0527.apsengine.capacity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class BottleneckDetector {

    static final BigDecimal THRESHOLD_PERCENT =
            new BigDecimal("80.00");

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    BottleneckAnalysis detect(
            long scheduleRunId,
            OffsetDateTime from,
            OffsetDateTime to,
            List<MachineCapacityInput> capacityInputs
    ) {
        if (scheduleRunId < 1) {
            throw new IllegalArgumentException(
                    "스케줄 실행 식별자는 1 이상이어야 합니다."
            );
        }
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "병목 진단 기간이 올바르지 않습니다."
            );
        }
        if (capacityInputs == null) {
            throw new IllegalArgumentException(
                    "설비 CAPA 입력은 null일 수 없습니다."
            );
        }

        List<CandidateDraft> drafts = new ArrayList<>();
        for (MachineCapacityInput input : capacityInputs) {
            if (input == null) {
                throw new IllegalArgumentException(
                        "설비 CAPA 입력 항목은 null일 수 없습니다."
                );
            }
            CandidateDraft draft = toCandidateDraft(input);
            if (draft != null) {
                drafts.add(draft);
            }
        }
        drafts.sort(
                Comparator.comparing(
                                CandidateDraft::noAvailableCapacity
                        )
                        .reversed()
                        .thenComparing(
                                CandidateDraft::exactUtilizationPercent,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                        .thenComparing(
                                draft -> draft.input().machineCode()
                        )
                        .thenComparingLong(
                                draft -> draft.input().machineId()
                        )
        );

        List<BottleneckCandidate> candidates =
                new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            candidates.add(drafts.get(index).toCandidate(index + 1));
        }
        return new BottleneckAnalysis(
                scheduleRunId,
                from,
                to,
                THRESHOLD_PERCENT,
                List.copyOf(candidates)
        );
    }

    private CandidateDraft toCandidateDraft(
            MachineCapacityInput input
    ) {
        if (input.availableMinutes() == 0) {
            if (input.loadMinutes() == 0) {
                return null;
            }
            return new CandidateDraft(
                    input,
                    null,
                    true,
                    BottleneckReason.NO_AVAILABLE_CAPACITY
            );
        }

        BigDecimal exactUtilizationPercent =
                BigDecimal.valueOf(input.loadMinutes())
                        .multiply(ONE_HUNDRED)
                        .divide(
                                BigDecimal.valueOf(
                                        input.availableMinutes()
                                ),
                                MathContext.DECIMAL128
                        );
        if (exactUtilizationPercent.compareTo(
                THRESHOLD_PERCENT
        ) < 0) {
            return null;
        }
        boolean capacityExceeded =
                input.loadMinutes() > input.availableMinutes();
        return new CandidateDraft(
                input,
                exactUtilizationPercent,
                capacityExceeded,
                capacityExceeded
                        ? BottleneckReason.CAPACITY_EXCEEDED
                        : BottleneckReason.HIGH_UTILIZATION
        );
    }

    private record CandidateDraft(
            MachineCapacityInput input,
            BigDecimal exactUtilizationPercent,
            boolean capacityExceeded,
            BottleneckReason reason
    ) {

        private boolean noAvailableCapacity() {
            return input.availableMinutes() == 0;
        }

        private BottleneckCandidate toCandidate(int rank) {
            BigDecimal displayUtilization =
                    exactUtilizationPercent == null
                            ? null
                            : exactUtilizationPercent.setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
            return new BottleneckCandidate(
                    rank,
                    input.machineId(),
                    input.machineCode(),
                    input.machineName(),
                    input.availableMinutes(),
                    input.loadMinutes(),
                    displayUtilization,
                    capacityExceeded,
                    reason
            );
        }
    }
}
