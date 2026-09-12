package com.mockinvestment.security.domain.cashwallet.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 계좌번호 생성: 777XXXXXXXX1 (12자리). X 는 0~9 무작위.
 * 앞 777 은 MOA 계좌 식별 접두, 끝 1 은 현금 계좌 구분자 (증권 계좌는 추후 다른 숫자).
 * 중복 확률은 10^8 분의 1 이라 무시하되, 암호화 저장이라 DB UNIQUE 로는 잡을 수 없다는 점은 알고 둔다.
 */
@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "777";
    private static final String SUFFIX = "1";
    private static final int RANDOM_DIGITS = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < RANDOM_DIGITS; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.append(SUFFIX).toString();
    }
}
