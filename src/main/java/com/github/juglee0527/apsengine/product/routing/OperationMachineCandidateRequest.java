package com.github.juglee0527.apsengine.product.routing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record OperationMachineCandidateRequest(
        @Min(value = 1, message = "후보 설비 ID는 1 이상이어야 합니다.")
        long machineId,

        @Min(value = 1, message = "후보 설비 우선순위는 1 이상이어야 합니다.")
        @Max(value = 1000, message = "후보 설비 우선순위는 1000 이하여야 합니다.")
        int priority
) {
}
