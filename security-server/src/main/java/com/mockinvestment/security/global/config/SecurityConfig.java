package com.mockinvestment.security.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockinvestment.security.global.exception.ErrorCode;
import com.mockinvestment.security.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 설정.
 * JWT 기반 stateless API 이므로 세션·CSRF·폼 로그인·Basic 인증은 전부 끈다.
 * 인증 필터(JWT 검증)는 로그인 API 구현 시 이 체인에 추가한다 (이슈 #9).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    /** 비밀번호 해시. BCrypt 는 salt 내장 + 의도적으로 느린 해시라 브루트포스에 강하다 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)        // 세션 쿠키를 안 쓰므로 CSRF 공격 면이 없다
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()   // 회원가입·로그인·토큰 재발급
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedEntryPoint()));
        return http.build();
    }

    /**
     * 미인증 요청에 대한 응답. 기본 동작은 빈 401 이지만 API 규칙에 맞춰 { code, message } JSON 으로 내려준다.
     * 이 지점은 @RestControllerAdvice 보다 앞(필터 단계)이라 핸들러에서 잡을 수 없어 직접 쓴다.
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.UNAUTHORIZED));
        };
    }
}
