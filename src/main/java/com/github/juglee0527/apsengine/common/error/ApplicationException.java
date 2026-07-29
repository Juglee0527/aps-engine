package com.github.juglee0527.apsengine.common.error;

import java.util.Objects;

public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public ApplicationException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApplicationException(
            ErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(resolveMessage(errorCode, message), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");

        if (message == null || message.isBlank()) {
            return errorCode.defaultMessage();
        }
        return message;
    }
}
