-- ============================================================
-- MOA V1: 초기 스키마
-- 사용자 / 인증 / 종목 / 시세 / 현금·증권 계좌 / 주문 / 체결 (10개 테이블)
--
-- 설계 원칙
--   - 금액·수량은 BIGINT 통일 (INT 오버플로 방지)
--   - history 테이블은 거래 후 잔액 스냅샷(balance_after, quantity_after) 보관 — 원장 대사(reconciliation)용
--   - 파생값은 저장하지 않는다 (평단가는 total_buy_amount / quantity 로 계산)
--   - 예약어 회피: user -> users, order -> orders
-- ============================================================

-- 사용자
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(72)  NOT NULL COMMENT 'BCrypt 해시',
    last_reset_at DATETIME(6)  NULL COMMENT '자산 리셋 시각 (1주일 1회 제한 검증용)',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Refresh Token (DB가 원본, Redis는 조회 캐시)
CREATE TABLE authentication (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL COMMENT 'refresh token 해시 (원문 저장 금지)',
    expires_at DATETIME(6)  NOT NULL,
    used_at    DATETIME(6)  NULL COMMENT 'Rotation 사용 시각. 사용된 토큰 재사용 = 탈취 신호 -> 유저 토큰 전체 무효화',
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_authentication_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_authentication_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_authentication_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 종목
CREATE TABLE stock (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(12)  NOT NULL COMMENT '종목코드 (예: 005930)',
    name       VARCHAR(100) NOT NULL,
    market     VARCHAR(20)  NOT NULL COMMENT 'KOSPI / KOSDAQ',
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 종목별 일자별 장 상태
CREATE TABLE market_status (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    stock_id          BIGINT      NOT NULL,
    trade_date        DATE        NOT NULL,
    base_price        BIGINT      NOT NULL COMMENT '기준가 (전일 종가)',
    upper_limit_price BIGINT      NOT NULL COMMENT '상한가 (기준가 +5%)',
    lower_limit_price BIGINT      NOT NULL COMMENT '하한가 (기준가 -5%)',
    open_price        BIGINT      NULL COMMENT '시가',
    close_price       BIGINT      NULL COMMENT '종가 (장외 시세 표시용)',
    created_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_market_status_stock_date UNIQUE (stock_id, trade_date),
    CONSTRAINT fk_market_status_stock FOREIGN KEY (stock_id) REFERENCES stock (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 현금 계좌 (1인 1계좌)
CREATE TABLE cash_wallet (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    account_no   VARCHAR(255) NOT NULL COMMENT 'AES-256-GCM 암호문 (랜덤 nonce + 인증태그 포함, base64)',
    balance      BIGINT       NOT NULL DEFAULT 0 COMMENT '예치금',
    tied_balance BIGINT       NOT NULL DEFAULT 0 COMMENT '매수 주문에 묶인 금액. 출금가능 = balance - tied_balance',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / SUSPENDED',
    version      BIGINT       NOT NULL DEFAULT 0 COMMENT '낙관적 락(@Version) - 정지/해제 등 충돌 드문 변경용. 잔액 갱신은 비관적 락 사용',
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cash_wallet_user UNIQUE (user_id),
    CONSTRAINT fk_cash_wallet_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_cash_wallet_tied CHECK (tied_balance <= balance)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 현금 거래 내역
CREATE TABLE cash_wallet_history (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    cash_wallet_id   BIGINT      NOT NULL,
    type             VARCHAR(20) NOT NULL COMMENT 'DEPOSIT / WITHDRAW / BUY_LOCK / BUY_UNLOCK / BUY_SETTLE / SELL_INCOME / RESET',
    amount           BIGINT      NOT NULL,
    balance_after    BIGINT      NOT NULL COMMENT '거래 후 잔액 스냅샷 (원장 대사용)',
    related_order_id BIGINT      NULL COMMENT '주문 관련 거래일 때 orders.id',
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cash_wallet_history_wallet FOREIGN KEY (cash_wallet_id) REFERENCES cash_wallet (id),
    INDEX idx_cash_wallet_history_wallet_created (cash_wallet_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 증권 계좌 (사용자 x 종목)
CREATE TABLE stock_wallet (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    stock_id         BIGINT      NOT NULL,
    quantity         BIGINT      NOT NULL DEFAULT 0 COMMENT '보유 수량',
    tied_quantity    BIGINT      NOT NULL DEFAULT 0 COMMENT '매도 주문에 묶인 수량. 매도가능 = quantity - tied_quantity',
    total_buy_amount BIGINT      NOT NULL DEFAULT 0 COMMENT '총 매입금액. 평단가 = total_buy_amount / quantity (파생값 미저장 원칙)',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / SUSPENDED',
    version          BIGINT      NOT NULL DEFAULT 0 COMMENT '낙관적 락(@Version)',
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_wallet_user_stock UNIQUE (user_id, stock_id),
    CONSTRAINT fk_stock_wallet_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_stock_wallet_stock FOREIGN KEY (stock_id) REFERENCES stock (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 증권 입출고 내역
CREATE TABLE stock_wallet_history (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    stock_wallet_id  BIGINT      NOT NULL,
    type             VARCHAR(20) NOT NULL COMMENT 'BUY_IN / SELL_OUT / SELL_LOCK / SELL_UNLOCK',
    quantity         BIGINT      NOT NULL,
    quantity_after   BIGINT      NOT NULL COMMENT '거래 후 보유량 스냅샷 (원장 대사용)',
    related_order_id BIGINT      NULL COMMENT '주문 관련 거래일 때 orders.id',
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_wallet_history_wallet FOREIGN KEY (stock_wallet_id) REFERENCES stock_wallet (id),
    INDEX idx_stock_wallet_history_wallet_created (stock_wallet_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 주문
CREATE TABLE orders (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    stock_id        BIGINT      NOT NULL,
    side            VARCHAR(4)  NOT NULL COMMENT 'BUY / SELL',
    price           BIGINT      NOT NULL,
    quantity        BIGINT      NOT NULL COMMENT '주문 수량',
    filled_quantity BIGINT      NOT NULL DEFAULT 0 COMMENT '체결된 수량 (부분 체결 추적)',
    status          VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / PARTIAL / FILLED / CANCELLED',
    idempotency_key VARCHAR(64) NULL COMMENT '멱등성 키 - 재시도 시 중복 체결 방지',
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_stock FOREIGN KEY (stock_id) REFERENCES stock (id),
    INDEX idx_orders_orderbook (stock_id, side, status) COMMENT 'Redis 오더북 재구성용',
    INDEX idx_orders_user_status (user_id, status) COMMENT '미체결 주문 조회용',
    INDEX idx_orders_user_created (user_id, created_at) COMMENT '주문 내역 페이징용'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 체결 내역
CREATE TABLE matches (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    stock_id       BIGINT      NOT NULL,
    buy_order_id   BIGINT      NOT NULL,
    sell_order_id  BIGINT      NOT NULL,
    buyer_user_id  BIGINT      NOT NULL COMMENT '조회용 비정규화 (orders JOIN 제거)',
    seller_user_id BIGINT      NOT NULL COMMENT '조회용 비정규화 (orders JOIN 제거)',
    price          BIGINT      NOT NULL COMMENT '체결가',
    quantity       BIGINT      NOT NULL COMMENT '체결 수량',
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_matches_stock FOREIGN KEY (stock_id) REFERENCES stock (id),
    CONSTRAINT fk_matches_buy_order FOREIGN KEY (buy_order_id) REFERENCES orders (id),
    CONSTRAINT fk_matches_sell_order FOREIGN KEY (sell_order_id) REFERENCES orders (id),
    INDEX idx_matches_stock_created (stock_id, created_at),
    INDEX idx_matches_buyer_created (buyer_user_id, created_at),
    INDEX idx_matches_seller_created (seller_user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
