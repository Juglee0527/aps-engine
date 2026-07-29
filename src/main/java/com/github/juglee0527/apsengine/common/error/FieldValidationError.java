package com.github.juglee0527.apsengine.common.error;

import java.util.Objects;

public record FieldValidationError(
        String field,
        String reason
) {

    public FieldValidationError {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}

