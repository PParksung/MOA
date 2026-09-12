package com.mockinvestment.security.domain.cashwallet.repository;

import com.mockinvestment.security.domain.cashwallet.entity.CashWalletHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashWalletHistoryRepository extends JpaRepository<CashWalletHistory, Long> {

    List<CashWalletHistory> findByCashWalletIdOrderByCreatedAtAsc(Long cashWalletId);
}
