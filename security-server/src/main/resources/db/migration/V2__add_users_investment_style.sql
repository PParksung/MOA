-- ============================================================
-- V2: 가입 시 투자 성향 선택 (이슈 #8)
--   SHORT_TERM(단타) → 자산 리셋 1주일 1회 / LONG_TERM(장투) → 3개월 1회
--   리셋 주기는 성향에서 파생되므로 컬럼으로 두지 않는다 (파생값 미저장 원칙)
--   기존 행이 있을 수 있어 DEFAULT 로 채운 뒤 DEFAULT 를 제거 → 이후 INSERT 는 반드시 명시
-- ============================================================
ALTER TABLE users
    ADD COLUMN investment_style VARCHAR(20) NOT NULL DEFAULT 'SHORT_TERM'
        COMMENT 'SHORT_TERM(단타) / LONG_TERM(장투). 리셋 주기 결정' AFTER password;

ALTER TABLE users
    ALTER COLUMN investment_style DROP DEFAULT;
