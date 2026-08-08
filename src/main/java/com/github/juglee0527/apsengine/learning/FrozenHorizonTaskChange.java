package com.github.juglee0527.apsengine.learning;

import java.time.OffsetDateTime;

public record FrozenHorizonTaskChange(
        String classification,
        String orderNumber,
        String operationCode,
        OffsetDateTime beforeStartAt,
        OffsetDateTime beforeEndAt,
        OffsetDateTime afterStartAt,
        OffsetDateTime afterEndAt,
        String reason
) {
}
