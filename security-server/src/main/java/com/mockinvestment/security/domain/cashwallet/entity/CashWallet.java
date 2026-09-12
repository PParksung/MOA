package com.mockinvestment.security.domain.cashwallet.entity;

import com.mockinvestment.security.global.jpa.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현금 계좌 (사용자당 1개, UNIQUE(user_id)).
 * balance: 예치금 / tiedBalance: 매수 주문으로 묶인 금액 / 출금 가능 = balance - tiedBalance
 * 불변식 tied_balance <= balance 는 DB CHECK 제약으로도 보장된다.
 */
@Entity
@Table(name = "cash_wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashWallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소유자. 연관관계(@OneToOne User) 대신 id 만 보관한다.
     * 계좌 로직은 항상 user_id 로 조회하고, User 객체 전체가 필요한 경우가 없어 지연 로딩 관리 비용을 피한다.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** AES-256-GCM 암호문 (랜덤 nonce + 인증태그 포함, Base64). 평문 계좌번호는 저장하지 않는다 */
    @Column(name = "account_no", nullable = false, length = 255)
    private String accountNo;

    @Column(nullable = false)
    private Long balance;

    @Column(name = "tied_balance", nullable = false)
    private Long tiedBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    /**
     * 낙관적 락. 정지/해제처럼 충돌이 드문 변경에 사용.
     * 잔액 갱신은 비관적 락(SELECT ... FOR UPDATE)으로 처리하므로 이 컬럼에 의존하지 않는다.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    private CashWallet(Long userId, String encryptedAccountNo) {
        this.userId = userId;
        this.accountNo = encryptedAccountNo;
        this.balance = 0L;
        this.tiedBalance = 0L;
        this.status = WalletStatus.ACTIVE;
    }

    /** 잔액 0원의 새 계좌. 초기 자금은 deposit 으로 넣어 거래내역이 남게 한다 */
    public static CashWallet open(Long userId, String encryptedAccountNo) {
        return new CashWallet(userId, encryptedAccountNo);
    }

    /** 입금. 거래 후 잔액을 반환한다 (history 의 balance_after 기록용) */
    public long deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("deposit amount must be positive: " + amount);
        }
        this.balance += amount;
        return this.balance;
    }

    public long getAvailableBalance() {
        return balance - tiedBalance;
    }
}
