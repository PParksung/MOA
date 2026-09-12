package com.mockinvestment.security.domain.user.controller;

import com.mockinvestment.security.domain.user.dto.SignupRequest;
import com.mockinvestment.security.domain.user.dto.SignupResponse;
import com.mockinvestment.security.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API. /api/v1/auth/** 는 SecurityConfig 에서 permitAll.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 회원가입. 성공 시 201 Created + { "user_id": n }
     * 400 유효성 오류 / 409 이메일 중복
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        Long userId = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignupResponse(userId));
    }
}
