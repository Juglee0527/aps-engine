package com.github.juglee0527.apsengine.product.routing;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OperationCreateRequest(
        @Min(value = 1, message = "Operation 순서는 1 이상이어야 합니다.")
        int sequence,

        @NotBlank(message = "Operation 코드는 필수입니다.")
        @Size(max = 50, message = "Operation 코드는 50자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "Operation 코드는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
        )
        String code,

        @NotBlank(message = "Operation 이름은 필수입니다.")
        @Size(max = 100, message = "Operation 이름은 100자를 초과할 수 없습니다.")
        String name,

        @Min(value = 1, message = "표준 가공시간은 1분 이상이어야 합니다.")
        @Max(value = 10080, message = "표준 가공시간은 10080분 이하여야 합니다.")
        int processingTimeMinutes,

        @Min(value = 1, message = "설비 ID는 1 이상이어야 합니다.")
        long machineId,

        @Size(min = 1, message = "후보 설비는 하나 이상이어야 합니다.")
        List<@Valid OperationMachineCandidateRequest> machineCandidates
) {

    public OperationCreateRequest {
        machineCandidates = machineCandidates == null
                ? null
                : List.copyOf(machineCandidates);
    }

    public OperationCreateRequest(
            int sequence,
            String code,
            String name,
            int processingTimeMinutes,
            long machineId
    ) {
        this(
                sequence,
                code,
                name,
                processingTimeMinutes,
                machineId,
                null
        );
    }
}
