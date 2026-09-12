package com.mockinvestment.security.global.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * created_at / updated_at 을 가진 엔티티의 공통 부모.
 * V1 스키마에서 두 컬럼은 NOT NULL 이고 DB DEFAULT 가 없으므로 애플리케이션이 반드시 채워야 한다.
 * (@MappedSuperclass: 테이블은 만들지 않고 컬럼만 자식 엔티티에 상속)
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
