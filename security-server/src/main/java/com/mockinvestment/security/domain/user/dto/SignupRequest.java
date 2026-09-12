package com.mockinvestment.security.domain.user.dto;

import com.mockinvestment.security.domain.user.entity.InvestmentStyle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청. JSON 은 snake_case (email, password, investment_style) — Jackson 전역 설정으로 변환.
 * record: 불변 + 생성자/접근자 자동. 요청 DTO 에 딱 맞다.
 */
public record SignupRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 32)
        String password,

        @NotNull
        InvestmentStyle investmentStyle
) {
}
