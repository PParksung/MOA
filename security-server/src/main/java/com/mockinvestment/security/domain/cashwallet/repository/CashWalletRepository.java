package com.mockinvestment.security.domain.cashwallet.repository;

import com.mockinvestment.security.domain.cashwallet.entity.CashWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashWalletRepository extends JpaRepository<CashWallet, Long> {

    Optional<CashWallet> findByUserId(Long userId);
}
