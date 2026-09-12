package com.mockinvestment.security.domain.user;

import com.mockinvestment.security.domain.cashwallet.entity.CashTxType;
import com.mockinvestment.security.domain.cashwallet.entity.CashWallet;
import com.mockinvestment.security.domain.cashwallet.entity.CashWalletHistory;
import com.mockinvestment.security.domain.cashwallet.repository.CashWalletHistoryRepository;
import com.mockinvestment.security.domain.cashwallet.repository.CashWalletRepository;
import com.mockinvestment.security.domain.cashwallet.service.CashWalletService;
import com.mockinvestment.security.domain.user.entity.InvestmentStyle;
import com.mockinvestment.security.domain.user.entity.User;
import com.mockinvestment.security.domain.user.repository.UserRepository;
import com.mockinvestment.security.global.crypto.AesGcmEncryptor;
import com.mockinvestment.security.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 API 통합 테스트: HTTP 요청 → 컨트롤러 → 서비스 → 실제 MySQL 까지 한 번에.
 * MockMvc 는 서블릿 컨테이너 없이 필터 체인(Spring Security 포함)까지 태운다.
 */
@AutoConfigureMockMvc
class SignupIntegrationTest extends IntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired CashWalletRepository cashWalletRepository;
    @Autowired CashWalletHistoryRepository historyRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AesGcmEncryptor encryptor;

    @BeforeEach
    void cleanUp() {
        // 컨테이너를 클래스 간에 공유하므로 매 테스트 전 초기화. FK 순서: history → wallet → user
        historyRepository.deleteAll();
        cashWalletRepository.deleteAll();
        userRepository.deleteAll();
    }

    private static String signupJson(String email, String password, String style) {
        return """
                {"email": "%s", "password": "%s", "investment_style": "%s"}
                """.formatted(email, password, style);
    }

    @Test
    void 회원가입하면_201과_user_id를_받고_계좌가_1000만원으로_개설된다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("moa@test.com", "password123", "SHORT_TERM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_id").isNumber());

        // 사용자: BCrypt 해시로 저장, 평문 아님
        User user = userRepository.findByEmail("moa@test.com").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("password123").startsWith("$2");
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
        assertThat(user.getInvestmentStyle()).isEqualTo(InvestmentStyle.SHORT_TERM);

        // 계좌: 잔액 1,000만원, 묶인 금액 0, 계좌번호는 암호화돼 있고 복호화하면 777...1
        CashWallet wallet = cashWalletRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(CashWalletService.INITIAL_BALANCE);
        assertThat(wallet.getTiedBalance()).isZero();
        assertThat(wallet.getAccountNo()).doesNotMatch("^777\\d{8}1$");
        assertThat(encryptor.decrypt(wallet.getAccountNo())).matches("^777\\d{8}1$");

        // 원장: DEPOSIT 1건, 거래 후 잔액 스냅샷 = 1,000만원
        List<CashWalletHistory> history = historyRepository.findByCashWalletIdOrderByCreatedAtAsc(wallet.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getType()).isEqualTo(CashTxType.DEPOSIT);
        assertThat(history.get(0).getAmount()).isEqualTo(CashWalletService.INITIAL_BALANCE);
        assertThat(history.get(0).getBalanceAfter()).isEqualTo(CashWalletService.INITIAL_BALANCE);
    }

    @Test
    void 같은_이메일로_다시_가입하면_409() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("dup@test.com", "password123", "LONG_TERM")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("dup@test.com", "another123", "SHORT_TERM")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("email already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void 비밀번호가_8자_미만이면_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("short@test.com", "short", "SHORT_TERM")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("password:")));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void 투자_성향이_목록에_없는_값이면_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("style@test.com", "password123", "YOLO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void 인증_없이_보호된_경로를_호출하면_401_JSON() throws Exception {
        mockMvc.perform(get("/api/v1/cash-wallet"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }
}
