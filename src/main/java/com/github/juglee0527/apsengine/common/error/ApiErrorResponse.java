package com.github.juglee0527.apsengine.common.error;

import java.util.List;
import java.util.Objects;

public record ApiErrorResponse(
        String code,
        String message,
        List<FieldValidationError> fieldErrors
) {

    public ApiErrorResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        fieldErrors = List.copyOf(fieldErrors);
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message) {
        return new ApiErrorResponse(errorCode.name(), message, List.of());
    }

    public static ApiErrorResponse withFieldErrors(
            ErrorCode errorCode,
            List<FieldValidationError> fieldErrors
    ) {
        return new ApiErrorResponse(
                errorCode.name(),
                errorCode.defaultMessage(),
                fieldErrors
        );
    }
}

