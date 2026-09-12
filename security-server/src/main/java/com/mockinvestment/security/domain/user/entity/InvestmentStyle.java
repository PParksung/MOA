package com.mockinvestment.security.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Period;

/**
 * 가입 시 선택하는 투자 성향. 자산 리셋 허용 주기를 결정한다.
 * 리셋 주기는 성향에서 파생되므로 DB 에는 성향만 저장한다 (파생값 미저장 원칙).
 */
@Getter
@RequiredArgsConstructor
public enum InvestmentStyle {

    /** 단타 — 1주일에 1회 리셋 */
    SHORT_TERM(Period.ofWeeks(1)),

    /** 장투 — 3개월에 1회 리셋 */
    LONG_TERM(Period.ofMonths(3));

    private final Period resetPeriod;
}
