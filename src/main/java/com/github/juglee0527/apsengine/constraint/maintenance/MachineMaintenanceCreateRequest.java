package com.github.juglee0527.apsengine.constraint.maintenance;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MachineMaintenanceCreateRequest(
        @NotNull(message = "정비 시작시각은 필수입니다.")
        OffsetDateTime startAt,

        @NotNull(message = "정비 종료시각은 필수입니다.")
        OffsetDateTime endAt,

        @NotBlank(message = "정비 사유는 필수입니다.")
        @Size(max = 200, message = "정비 사유는 200자 이하여야 합니다.")
        String reason
) {
}
