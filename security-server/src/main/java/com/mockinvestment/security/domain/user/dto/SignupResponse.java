package com.mockinvestment.security.domain.user.dto;

/** 회원가입 응답. { "user_id": 1 } */
public record SignupResponse(Long userId) {
}
