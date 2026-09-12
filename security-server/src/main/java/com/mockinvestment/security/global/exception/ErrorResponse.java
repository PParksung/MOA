package com.mockinvestment.security.global.exception;

import org.springframework.http.HttpStatus;

/** 에러 응답 바디. { "code": "412", "message": "cash wallet is blocked." } */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(String.valueOf(status.value()), message);
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode.getStatus(), errorCode.getMessage());
    }
}
