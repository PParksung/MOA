package com.mockinvestment.security.domain.cashwallet.entity;

/**
 * 현금 거래 유형 (cash_wallet_history.type).
 * 잔액에 영향을 주는 모든 사건을 빠짐없이 기록하는 것이 원장(ledger)의 기본.
 */
public enum CashTxType {
    /** 입금 (가입 시 초기 자금 지급 포함) */
    DEPOSIT,
    /** 출금 */
    WITHDRAW,
    /** 매수 주문 접수 — 주문 금액을 tied_balance 로 묶음 */
    BUY_LOCK,
    /** 매수 주문 취소 — 묶인 금액 해제 */
    BUY_UNLOCK,
    /** 매수 체결 — 묶인 금액 차감 확정 */
    BUY_SETTLE,
    /** 매도 체결 — 대금 입금 */
    SELL_INCOME,
    /** 자산 리셋 — 초기 자금으로 초기화 */
    RESET
}
