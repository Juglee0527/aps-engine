package com.github.juglee0527.apsengine.order;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductionOrderCreateRequest(
        @NotBlank(message = "생산오더 번호는 필수입니다.")
        @Size(max = 50, message = "생산오더 번호는 50자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "생산오더 번호는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
        )
        String orderNumber,

        @Min(value = 1, message = "Routing ID는 1 이상이어야 합니다.")
        long routingId,

        @Positive(message = "생산수량은 1 이상이어야 합니다.")
        @Max(value = 1_000_000, message = "생산수량은 1000000 이하여야 합니다.")
        long quantity,

        @NotNull(message = "투입 가능 시각은 필수입니다.")
        OffsetDateTime releaseAt,

        @NotNull(message = "납기시각은 필수입니다.")
        OffsetDateTime dueAt,

        @Min(value = 1, message = "우선순위는 1 이상이어야 합니다.")
        @Max(value = 100, message = "우선순위는 100 이하여야 합니다.")
        int priority
) {
}
