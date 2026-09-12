package com.mockinvestment.security.domain.cashwallet.entity;

/** 계좌 상태. SUSPENDED 면 입출금·주문 전부 거부 */
public enum WalletStatus {
    ACTIVE,
    SUSPENDED
}
