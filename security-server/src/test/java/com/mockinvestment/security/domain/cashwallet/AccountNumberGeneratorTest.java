package com.mockinvestment.security.domain.cashwallet;

import com.mockinvestment.security.domain.cashwallet.service.AccountNumberGenerator;
import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNumberGeneratorTest {

    private final AccountNumberGenerator generator = new AccountNumberGenerator();

    @RepeatedTest(20)
    void 계좌번호는_777로_시작해_1로_끝나는_12자리_숫자다() {
        assertThat(generator.generate()).matches("^777\\d{8}1$");
    }
}
