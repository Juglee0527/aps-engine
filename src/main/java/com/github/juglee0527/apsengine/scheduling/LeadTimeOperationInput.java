package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.Objects;

record LeadTimeOperationInput(
        long productionOrderId,
        String orderNumber,
        long productId,
        String productCode,
        OffsetDateTime releaseAt,
        OffsetDateTime endAt,
        long processingMinutes,
        long changeoverMinutes
) {

    LeadTimeOperationInput {
        if (productionOrderId < 1 || productId < 1) {
            throw new IllegalArgumentException(
                    "생산오더와 품목 식별자는 1 이상이어야 합니다."
            );
        }
        if (orderNumber == null || orderNumber.isBlank()
                || productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException(
                    "생산오더 번호와 품목 코드는 필수입니다."
            );
        }
        Objects.requireNonNull(releaseAt, "releaseAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        if (endAt.isBefore(releaseAt)) {
            throw new IllegalArgumentException(
                    "공정 완료시각은 생산오더 투입 가능시각보다 이전일 수 없습니다."
            );
        }
        if (processingMinutes < 1 || changeoverMinutes < 0) {
            throw new IllegalArgumentException(
                    "가공시간은 1분 이상이고 Changeover Time은 0분 이상이어야 합니다."
            );
        }
    }
}
