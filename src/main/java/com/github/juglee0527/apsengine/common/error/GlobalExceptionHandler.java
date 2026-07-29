package com.github.juglee0527.apsengine.common.error;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException exception
    ) {
        ErrorCode errorCode = exception.errorCode();
        ApiErrorResponse response =
                ApiErrorResponse.of(errorCode, exception.getMessage());

        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            BindException exception
    ) {
        List<FieldValidationError> fieldErrors = new ArrayList<>();

        for (FieldError fieldError
                : exception.getBindingResult().getFieldErrors()) {
            String reason = fieldError.getDefaultMessage();
            if (reason == null || reason.isBlank()) {
                reason = "잘못된 값입니다.";
            }
            fieldErrors.add(new FieldValidationError(
                    fieldError.getField(),
                    reason
            ));
        }

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ApiErrorResponse response =
                ApiErrorResponse.withFieldErrors(errorCode, fieldErrors);

        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        FieldValidationError fieldError = new FieldValidationError(
                exception.getName(),
                "요청값 형식이 올바르지 않습니다."
        );
        ApiErrorResponse response = ApiErrorResponse.withFieldErrors(
                errorCode,
                List.of(fieldError)
        );

        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage() {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ApiErrorResponse response =
                ApiErrorResponse.of(errorCode, errorCode.defaultMessage());

        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound() {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        ApiErrorResponse response =
                ApiErrorResponse.of(errorCode, errorCode.defaultMessage());

        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unhandled exception", exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ApiErrorResponse response =
                ApiErrorResponse.of(errorCode, errorCode.defaultMessage());

        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }
}
