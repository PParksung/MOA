package com.mockinvestment.security.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 에러 목록. 응답은 { "code": "409", "message": "email already exists" } 형식 (API 규칙).
 * code 는 HTTP 상태 코드 문자열, message 는 영어로 간결히.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "invalid input"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "email already exists"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");

    private final HttpStatus status;
    private final String message;
}
