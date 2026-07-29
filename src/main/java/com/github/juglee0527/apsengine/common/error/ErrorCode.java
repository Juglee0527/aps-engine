package com.github.juglee0527.apsengine.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "현재 상태에서는 요청을 처리할 수 없습니다."),
    FACTORY_CODE_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 공장 코드입니다."),
    FACTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "공장을 찾을 수 없습니다."),
    FACTORY_INACTIVE(HttpStatus.CONFLICT, "비활성 공장에는 생산라인을 등록할 수 없습니다."),
    PRODUCTION_LINE_CODE_DUPLICATED(
            HttpStatus.CONFLICT,
            "해당 공장에 이미 등록된 생산라인 코드입니다."
    ),
    PRODUCTION_LINE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "생산라인을 찾을 수 없습니다."
    ),
    PRODUCTION_LINE_INACTIVE(
            HttpStatus.CONFLICT,
            "비활성 생산라인에는 설비를 등록할 수 없습니다."
    ),
    MACHINE_CODE_DUPLICATED(
            HttpStatus.CONFLICT,
            "해당 생산라인에 이미 등록된 설비 코드입니다."
    ),
    MACHINE_NOT_FOUND(HttpStatus.NOT_FOUND, "설비를 찾을 수 없습니다."),
    PRODUCT_CODE_DUPLICATED(
            HttpStatus.CONFLICT,
            "이미 등록된 품목 코드입니다."
    ),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
