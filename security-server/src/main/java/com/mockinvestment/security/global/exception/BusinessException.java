package com.mockinvestment.security.global.exception;

import lombok.Getter;

/** 서비스 계층에서 던지는 예외. GlobalExceptionHandler 가 ErrorCode 의 상태 코드로 응답을 만든다 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
