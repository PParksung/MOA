package com.mockinvestment.security.domain.cashwallet.service;

import com.mockinvestment.security.domain.cashwallet.entity.CashTxType;
import com.mockinvestment.security.domain.cashwallet.entity.CashWallet;
import com.mockinvestment.security.domain.cashwallet.entity.CashWalletHistory;
import com.mockinvestment.security.domain.cashwallet.repository.CashWalletHistoryRepository;
import com.mockinvestment.security.domain.cashwallet.repository.CashWalletRepository;
import com.mockinvestment.security.global.crypto.AesGcmEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashWalletService {

    /** 가입 시 지급하는 가상 초기 자금. 100만원은 대형주 몇 주로 끝나 분산 연습이 안 된다는 피드백으로 1,000만원 */
    public static final long INITIAL_BALANCE = 10_000_000L;

    private final CashWalletRepository cashWalletRepository;
    private final CashWalletHistoryRepository historyRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AesGcmEncryptor encryptor;

    /**
     * 계좌 개설 + 초기 자금 입금 + 거래내역 기록.
     * 호출자(회원가입)의 트랜잭션에 참여한다 — 사용자 저장과 계좌 개설은 함께 성공하거나 함께 실패해야 한다.
     */
    @Transactional
    public CashWallet openWithInitialBalance(Long userId) {
        String accountNo = accountNumberGenerator.generate();
        CashWallet wallet = cashWalletRepository.save(CashWallet.open(userId, encryptor.encrypt(accountNo)));

        long balanceAfter = wallet.deposit(INITIAL_BALANCE);
        historyRepository.save(CashWalletHistory.of(
                wallet.getId(), CashTxType.DEPOSIT, INITIAL_BALANCE, balanceAfter, null));

        return wallet;
    }
}
