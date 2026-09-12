package com.mockinvestment.security.domain.user.service;

import com.mockinvestment.security.domain.cashwallet.service.CashWalletService;
import com.mockinvestment.security.domain.user.dto.SignupRequest;
import com.mockinvestment.security.domain.user.entity.User;
import com.mockinvestment.security.domain.user.repository.UserRepository;
import com.mockinvestment.security.global.exception.BusinessException;
import com.mockinvestment.security.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CashWalletService cashWalletService;

    /**
     * 회원가입: 사용자 저장 + 현금 계좌 개설 + 초기 자금 지급을 한 트랜잭션으로.
     * 어느 하나라도 실패하면 전부 롤백 — "가입은 됐는데 계좌가 없는" 사용자를 만들지 않는다.
     */
    @Transactional
    public Long signup(SignupRequest request) {
        // 1차 검증: 친절한 409 를 빨리 돌려주기 위한 사전 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .investmentStyle(request.investmentStyle())
                .build();

        try {
            // IDENTITY 전략이라 save 시점에 즉시 INSERT → UNIQUE 위반이면 여기서 터진다
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 2차 방어: 같은 이메일로 동시에 가입하면 existsByEmail 을 둘 다 통과할 수 있다.
            // 최종 보루는 DB 의 uk_users_email — 그 위반을 다시 409 로 번역한다
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        cashWalletService.openWithInitialBalance(user.getId());
        return user.getId();
    }
}
