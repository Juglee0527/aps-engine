package com.github.juglee0527.apsengine.constraint.changeover;

import jakarta.validation.constraints.Min;

public record ChangeoverTimeCreateRequest(
        @Min(value = 1, message = "이전 품목 ID는 1 이상이어야 합니다.")
        long fromProductId,

        @Min(value = 1, message = "다음 품목 ID는 1 이상이어야 합니다.")
        long toProductId,

        @Min(value = 0, message = "Changeover Time은 0분 이상이어야 합니다.")
        int changeoverMinutes
) {
}
