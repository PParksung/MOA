package com.mockinvestment.security.domain.user.entity;

import com.mockinvestment.security.global.jpa.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 (테이블 users — user 는 일부 DB 에서 예약어라 복수형).
 * password 는 BCrypt 해시만 저장한다. 평문은 서비스 계층에서 인코딩 후 넘어온다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 스펙상 기본 생성자 필요. 외부에서 빈 객체 생성은 막는다
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT 위임
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt 해시 (60자 고정). 컬럼은 여유를 둔 VARCHAR(72) */
    @Column(nullable = false, length = 72)
    private String password;

    @Enumerated(EnumType.STRING) // ORDINAL 은 enum 순서가 바뀌면 데이터가 깨진다
    @Column(name = "investment_style", nullable = false, length = 20)
    private InvestmentStyle investmentStyle;

    /** 마지막 자산 리셋 시각. NULL = 아직 리셋한 적 없음 */
    @Column(name = "last_reset_at")
    private LocalDateTime lastResetAt;

    @Builder
    private User(String email, String password, InvestmentStyle investmentStyle) {
        this.email = email;
        this.password = password;
        this.investmentStyle = investmentStyle;
    }
}
