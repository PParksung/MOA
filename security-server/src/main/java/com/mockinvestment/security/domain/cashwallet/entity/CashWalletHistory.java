package com.mockinvestment.security.domain.cashwallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 현금 거래 내역 (원장). 한 번 쓰면 수정하지 않는다 — updated_at 이 없는 이유.
 * balanceAfter: 거래 직후 잔액 스냅샷. 내역을 순서대로 더해 현재 잔액과 대사(reconciliation)할 수 있다.
 */
@Entity
@Table(name = "cash_wallet_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashWalletHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cash_wallet_id", nullable = false)
    private Long cashWalletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashTxType type;

    /** 거래 금액 (항상 양수. 방향은 type 이 말해준다) */
    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    /** 주문에서 비롯된 거래면 orders.id. 입금/출금/리셋은 NULL */
    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CashWalletHistory(Long cashWalletId, CashTxType type, long amount, long balanceAfter, Long relatedOrderId) {
        this.cashWalletId = cashWalletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.relatedOrderId = relatedOrderId;
    }

    public static CashWalletHistory of(Long cashWalletId, CashTxType type, long amount, long balanceAfter, Long relatedOrderId) {
        return new CashWalletHistory(cashWalletId, type, amount, balanceAfter, relatedOrderId);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
