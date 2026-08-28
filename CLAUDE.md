# MOA 프로젝트 가이드

## 프로젝트 개요

**MOA (Mock Onboarding to Assets)**
> 실제 시세로 연습하는 AI 모의투자 플랫폼
> 슬로건: "투자, 먼저 모아보세요."

### 문제 정의
주식 투자에 관심은 있지만 실제 돈을 잃을까봐 시작을 못 하는 사람들이 많다.
기존 증권사 모의투자 서비스의 문제:
- 실제 계좌 개설 + 본인인증 필요 → 진입 장벽 높음
- HTS/MTS UI가 전문 트레이더용 → 입문자에게 복잡함
- 투자 후 피드백 없음 → 뭘 배웠는지 모름

### 해결책
- 이메일만으로 즉시 가입, 1,000만원 가상 자금 자동 지급
- 입문자 친화적 심플한 UI (단, 차트·호가창·보조지표는 실제 증권앱 수준으로)
- **나만의 투자 규칙을 정하고, AI가 규칙 준수 여부와 투자 습관을 리포트로 피드백** (핵심 차별점)
- 수수료·세금·기업 분석·주가 예측은 의도적으로 제외 (아래 "고려하지 않는 것" 참고)

### 포트폴리오 목적
충남대 컴공 4학년, 은행 IT 공채(KB/신한/하나/우리) 준비.
금융 도메인 이해 + 백엔드 시스템 설계 + AI 연동 역량을 보여주는 프로젝트.
개발 공부 목적으로 진행 중이므로 코드 작성 시 설명을 곁들이며 진행.

---

## 로컬 개발 환경
- Java: OpenJDK 21 (Eclipse Temurin)
- MySQL: 8.4.0
- Node.js: v22.14.0
- Docker: 28.0.1
- OS: macOS (Apple Silicon)

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Backend | Spring Boot 3.5.5, Spring Data JPA, Spring Data Redis |
| Database | MySQL 8.4 |
| Cache | Redis 7 |
| Auth | JWT (jjwt 0.12.7), BCrypt (비밀번호), AES-256-GCM (계좌번호) |
| Frontend | React + TypeScript, Tailwind CSS |
| 실시간 시세 | 한국투자증권(KIS) Open API — 실전투자 App Key 보유. 현재가 + 호가 10단계(가격·잔량) |
| 실시간 푸시 | WebSocket — 시세·체결 알림 (Nginx·Gateway가 WS 프록시) |
| 메시징 | Kafka — 체결 이벤트 발행/구독 (exchange → ai-server) |
| AI 분석 | LLM API (규칙 준수표·습관 수치를 입력으로 총평 생성. 예측·추천 없음) |
| API Gateway | Spring Cloud Gateway 2025.0.2 (MVC) |
| Reverse Proxy | Nginx |
| DB 마이그레이션 | Flyway |
| 장애 격리 | Resilience4j (KIS API 서킷브레이커) |
| Rate Limit | Bucket4j (Gateway) |
| 테스트 | Testcontainers (MySQL/Redis 통합 테스트) |
| 모니터링 | Prometheus + Grafana, Spring Actuator |
| 부하 테스트 | nGrinder |
| 빌드 | Gradle |
| 컨테이너 | Docker, Docker Compose |

---

## 시스템 아키텍처

```
[Frontend :5173]
       ↕                ← 요청(HTTP) + 실시간 푸시(WebSocket)
  [Nginx :80]           ← 리버스 프록시 + React 정적파일 서빙 + WS 프록시
       ↕
  [Gateway :8082]       ← 라우팅 + WS 프록시
   ├── /api/v1/stock/**, /api/v1/market/** → market-server
   ├── /api/v1/ai/**                       → ai-server
   └── /api/**                             → security-server
       ↓                    ↓                      ↓
[security-server]   [market-server]          [ai-server]
     :8080               :8083                  :8084
       ↓                    ↓                      ↓
       └──────── MySQL :3306 ───────────────────────┘
       ↓                    ↓
  Redis :6379          Redis :6379 (시세 캐싱)
       ↓
[exchange-server :8081]  ← security-server만 호출. 외부 미노출
       ↓                    └─ 체결 이벤트 발행 → [Kafka :9092] → ai-server 구독
  Redis :6379 (오더북)

[Prometheus :9090 / Grafana :3000]  ← 전 서버 Actuator 메트릭 수집
```

### Nginx (port 80)
외부 단일 진입점.
- `/` → React 빌드 파일 직접 서빙
- `/api/**` → Gateway로 리버스 프록시
- SSL 종료는 Docker Compose 단계에서 구성

### Gateway (port 8082)
Spring Cloud Gateway MVC. 라우팅 규칙:
- `/api/v1/stock/**`, `/api/v1/market/**` → market-server
- `/api/v1/ai/**` → ai-server
- `/api/**` (나머지) → security-server
- exchange-server는 Gateway 라우팅 대상 아님 (내부 전용)

### security-server (port 8080)
인증/계좌/주문 담당:
- 사용자 인증/인가 (JWT)
- 현금 계좌 관리 (가입 시 1,000만원 자동 지급)
- 증권 계좌 관리
- 주문 접수 → exchange-server로 전달
- 오더북 조회 (exchange-server에서 가져옴)
- 포트폴리오

### exchange-server (port 8081)
거래 체결 엔진. security-server만 호출 가능. 외부 미노출:
- 미체결 지정가 주문 대기열 관리 (Redis 기반, ZSET/LIST/HASH)
- **실제 호가 잔량 기준 가상 체결**: market-server의 호가 갱신마다 지정가에 도달한 대기 주문을 실제 잔량 범위 안에서 체결. 잔량 초과분은 부분 체결 후 대기. 같은 가격의 유저 주문끼리는 FIFO
  - 유저간 상대매매를 쓰지 않는 이유: 유저가 적으면 반대 주문이 없어 영원히 미체결이고, 유저끼리 체결되면 실제 시세와 괴리됨
  - 대형주는 잔량이 커서 사실상 즉시 전량 체결, 소형주는 잔량만큼만 체결 → 슬리피지·부분 체결을 자연스럽게 경험
- 주문 취소 처리
- 체결 이벤트 Kafka 발행
- 주문 접수는 24시간, 체결은 장중(호가가 있을 때)만

### market-server (port 8083)
실시간 시세 담당:
- KIS WebSocket 실시간 시세 수신 (현재가 + 호가 10단계 가격·잔량)
- 종목 목록 + 현재가 + 등락률 제공
- 호가 갱신을 exchange-server에 전달 (체결 기준) + 프론트 호가창 제공
- 보조지표 계산 API (이동평균선, MACD, 거래량) — 계산은 서버, 그리기는 프론트 차트 라이브러리(lightweight-charts)
- 장중/장외 상태 관리
- 시세 Redis 캐싱

### ai-server (port 8084)
AI 투자 패턴 분석 담당:
- 투자 규칙 준수 체크: 사용자가 정한 규칙(`investment_rule`)과 거래 내역을 대조 → 규칙별 준수/위반 횟수, 위반 거래 목록 (순수 계산, LLM 미사용)
- 습관 수치 계산: 평균 보유기간, 승률, 손절 평균, 물타기 횟수, 시간대별 매수 분포 (순수 계산)
- LLM API 호출 → 위 수치를 입력으로 "총평 + 가장 많이 어긴 규칙에 대한 조언" 한 문단 생성. 종목 추천·주가 예측은 하지 않음
  - 수치가 다 계산된 상태로 들어가므로 무료 티어 LLM으로도 품질이 크게 흔들리지 않음
  - LLM 호출부는 반드시 `LlmClient` 인터페이스로 추상화 (GeminiClient 구현체) — v1.0 이후 파인튜닝 모델 교체 실험 대비
  - 리포트 생성 시 입력(패턴 요약 JSON)·출력(리포트) 로깅 — 파인튜닝 평가셋 재료
- FDS (이상거래 탐지): 룰 기반 + 고전 ML(scikit-learn 등) — LLM 파인튜닝 대상 아님
- Kafka 체결 이벤트 구독 (체결 흐름과 분석 흐름 분리)
- v1.0 이후: 파인튜닝 실험 (상세 계획은 project-overview.html의 "AI 파인튜닝 실험 계획" 섹션 참고)

---

## 핵심 기능

### 1. 사용자 인증
- 회원가입: 이메일 + 비밀번호 (BCrypt 해싱 — SHA-512는 빠른 범용 해시라 브루트포스에 취약하여 변경)
- 가입 시: 현금 계좌 자동 개설 + 1,000만원 자동 지급
  - 1,000만원인 이유: 100만원이면 대형주 몇 주밖에 못 사 분산·리밸런싱 연습이 안 됨 (금융권 지인 피드백: 최소 1,000만원)
- 가입 시 투자 성향 선택: 단타 / 장투 → 리셋 주기 결정
- 로그인: JWT 발급 (access 5분, refresh 1주일)
- 토큰 재발급: Refresh Token Rotation — 재발급 시 refresh token도 교체, 이전 토큰 재사용 감지 시 해당 유저 토큰 전체 무효화
- 자산 리셋: 1,000만원으로 초기화. 단타는 1주일에 1회, 장투는 3개월에 1회 제한
  - 완전 무제한 리셋은 투자 의미가 없어지므로 제한을 둠
  - 리셋 주기 동안은 현실처럼 신중하게 투자하도록 유도

### 2. 현금 계좌
- 계좌번호 AES-256-GCM 암호화 저장 (777XXXXXXXX1 형식) — CBC는 무결성 검증이 없어 AEAD 방식인 GCM으로 변경. 암호화 키는 환경변수 주입(yml 평문 금지)
- 잔액 조회 (예치금 / 매수 묶인 금액 / 출금 가능 금액)
- 입금 / 출금 / 거래 내역 조회 (페이징)
- 계좌 정지 / 해제

### 3. 증권 계좌
- 종목별 계좌 개설
- 잔고 조회 (보유량 / 매도 묶인 수량 / 매도 가능 수량)
- 계좌 정지 / 해제

### 4. 실시간 시세 (KIS API)
- 장중(09:00~15:30 평일): KIS WebSocket 실시간 시세 + 호가 10단계
- 장외: 마지막 종가 유지, 호가 없음 (체결 불가)
- 종목 목록 + 현재가 + 등락률 제공
- 차트: 캔들 + 거래량 + 보조지표(이동평균선, MACD). 지표 계산은 market-server, 렌더링은 프론트

### 5. 주문 및 체결
- 매수 / 매도 주문 접수
- 호가 단위 검증 (가격 구간별), 상하한가 검증 (±5%)
- 실제 호가 잔량 기준 가상 체결: 매수 지정가 ≥ 실제 매도1호가(매도는 반대)면 체결, 실제 잔량 초과분은 부분 체결 후 대기. 같은 가격의 유저 주문끼리는 FIFO
- 1차는 지정가 주문만. 시장가는 이후
- 미체결 주문 조회 / 체결 내역 조회 / 주문 취소

### 6. 오더북 (Redis) — 미체결 주문 대기열
- 역할: 유저들의 미체결 지정가 주문을 가격순으로 보관하고, 호가 갱신 때 지정가에 도달한 주문을 찾아 체결
- `{stock_id}:{side}` → ZSET (가격 목록, score=가격) — 최우선 호가를 O(log N)에 조회 (LIST는 전체 스캔 필요하여 변경)
- `{stock_id}:{side}:{price}` → LIST (주문 FIFO)
- `{stock_id}:{side}:total-unit` → HASH (가격별 잔량)
- 체결 1건 = 3개 키 동시 갱신 → Lua 스크립트로 원자적 실행
- DB가 source of truth: Redis 유실 시 MySQL 미체결 주문으로 오더북 재구성 가능

### 7. 투자 규칙 + AI 피드백 (핵심 차별점)
- 사용자가 자기 투자 규칙을 정한다. 예: 손실 -5%면 손절 / 한 종목 비중 30% 이하 / 장 시작 30분 내 매수 금지 / 최소 보유 3일
  - 규칙은 정형 데이터(유형 + 임계값)로 저장(`investment_rule`) → 거래마다 자동 대조 가능
  - 초보에게 가장 필요한 건 "나만의 규칙을 세우고 지키는 연습"이라는 금융권 지인 피드백 반영
- 리포트 구성
  1. 규칙 준수표: 규칙별 지킨 횟수 / 어긴 횟수 / 어긴 거래 목록
  2. 습관 수치: 평균 보유기간, 승률, 손절 평균, 물타기 횟수, 시간대별 매수 분포
  3. AI 총평: 1·2를 입력으로 한 문단 (가장 많이 어긴 규칙 하나에 대한 조언 포함)
- 하지 않는 것: 종목 추천, 주가 예측, 기업 재무·뉴스 분석 (투자 권유가 되고, 데이터 비용·환각 위험)
- FDS(이상거래 탐지): 룰 기반(단시간 동일 종목 반복 주문, 자전거래 패턴) + LLM 분석 조합

### 8. 포트폴리오
- 보유 종목 + 평가손익 + 수익률 + 전체 자산 현황

### 9. 실시간 알림 (WebSocket)
- 시세·체결 알림을 서버 → 브라우저로 푸시
- Nginx와 Gateway가 WebSocket 프록시 담당

### 10. 차트·호가창
- 호가창: 실제 KIS 호가 10단계(가격·잔량) 그대로 표시 — 체결 기준과 같은 데이터
- 차트: lightweight-charts로 캔들·거래량 렌더링, 보조지표는 market-server API로 받아 오버레이
- 원칙: 지표 계산 로직은 서버, 그리기는 라이브러리 — 백엔드 포트폴리오가 프론트 프로젝트로 변질되지 않게

---

## 고려하지 않는 것 (의도적 제외)

| 항목 | 이유 |
|------|------|
| 수수료·세금 | 기존 모의투자 서비스 대부분이 반영하지 않는 관행. 초보가 배울 건 0.015% 수수료가 아니라 매매 판단. 필요해지면 체결 시 정률 차감 한 줄로 추가 가능한 구조 |
| 시장가 주문 (1차) | 지정가 + 호가 잔량 체결로 충분. 시장가는 호가 여러 단계를 순차 소진하는 로직이라 2차 |
| 기업 재무·뉴스 분석, 주가 예측 | 투자 권유가 됨. 데이터 유료, LLM 환각 위험. "예측하지 않고 습관만 본다"가 서비스 정체성 |
| 주식 외 자산(채권·현금 비중 추천 등) | 서비스 범위 밖 |
| 내 주문의 시장 영향 | 1,000만원 규모는 실제 시장에서도 영향 없음. 소형주의 비현실성은 호가 잔량 기반 부분 체결로 보완 |

---

## DB 스키마 (MySQL - security DB)

| 테이블 | 설명 |
|--------|------|
| user | 사용자 정보 |
| authentication | refresh token 저장 |
| stock | 종목 정보 |
| market_status | 종목별 일자별 장 상태 (기준가, 상하한가, 시가, 종가 등) |
| cash_wallet | 현금 계좌 |
| cash_wallet_history | 현금 입출금 내역 |
| stock_wallet | 증권 계좌 (종목별) |
| stock_wallet_history | 증권 입출고 내역 |
| order | 주문 |
| matches | 체결 내역 |
| investment_rule | 사용자 투자 규칙 (유형 + 임계값) — **V2 예정** |

V2 예정 변경: `users`에 투자 성향(단타/장투)·리셋 주기 컬럼 추가, `investment_rule` 테이블 신설

### DB 설계 원칙
- `orders.status` 컬럼 명시 (PENDING / PARTIAL / FILLED / CANCELLED) + 인덱스
- 금액·수량 타입은 BIGINT 통일 (INT 최대 약 21억 — 곱셈 오버플로 방지)
- wallet history에 거래 후 잔액(`balance_after`) 스냅샷 기록 — 금융권 원장(ledger)의 기본, 잔액 추적·대사 가능
- 스키마는 Flyway 마이그레이션으로 버전 관리
- MySQL 인스턴스는 공유하되 서버별 스키마/계정 권한 분리 (MSA shared database 안티패턴 보완)

---

## 트랜잭션 & 동시성 설계 원칙

- **잔액 갱신은 비관적 락** (`SELECT ... FOR UPDATE`): 동시 주문 시 tied_savings race condition 방지. 돈 관련 로직은 충돌 시 실패하면 안 됨
- **충돌이 드문 영역은 낙관적 락** (JPA `@Version`): 계좌 정지/해제 등
- **Saga 패턴 (보상 트랜잭션)**: 주문 흐름이 `돈 묶기(MySQL) → 체결(exchange Redis) → 결과 반영(MySQL)`로 저장소를 넘나들므로, 체결 거절/실패 시 묶은 금액을 되돌리는 보상 단계를 명시적으로 정의
- **타임아웃 처리**: exchange-server 응답 유실 시 주문 상태를 UNKNOWN으로 두고 재조회로 확정
- **멱등성**: order_id 기반 멱등 처리 — 재시도해도 중복 체결 방지
- **대사(Reconciliation) 배치**: DB 주문 상태 ↔ Redis 오더북 주기적 비교·검증

---

## 보안 설계 원칙

- 비밀번호: BCrypt (Spring Security `BCryptPasswordEncoder`)
- 계좌번호: AES-256-GCM (AEAD — 변조 감지 포함)
- 암호화 키: 환경변수 주입, application.yml 평문 저장 금지 (장기: Vault)
- Refresh Token Rotation + 재사용 감지 시 전체 무효화
- exchange-server 내부 API 키 인증 (Docker 네트워크 분리만으로는 부족)
- Gateway rate limiting (Bucket4j)

---

## API 규칙

- 모든 요청/응답 Body: JSON, 필드명 snake_case
- 인증 필요 API: `Authorization: Bearer {access_token}` 헤더
- 에러 응답 형식:
```json
{ "code": "401", "message": "unauthorized" }
```

---

## 개발 규칙 (상세 내용은 CONTRIBUTING.md 참고)

### 브랜치 전략
```
main         ← 배포 가능한 완성 코드. PR로만 머지.
develop      ← 개발 통합 브랜치. PR로만 머지.
feat/1-xxx   ← 기능 개발 ({타입}/{이슈번호}-{설명})
fix/12-xxx   ← 버그 수정
```

### 커밋 컨벤션
```
feat: 새 기능
fix: 버그 수정
docs: 문서
refactor: 리팩토링
chore: 설정, 빌드
test: 테스트
```

### 포트
| 서버 | 포트 |
|------|------|
| Nginx | 80 |
| gateway | 8082 |
| security-server | 8080 |
| exchange-server | 8081 |
| market-server | 8083 |
| ai-server | 8084 |
| frontend | 5173 |
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 9092 |
| Prometheus | 9090 |
| Grafana | 3000 |

### 패키지 구조
```
com.mockinvestment.{server}
├── domain
│   └── {도메인명}
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
└── global
    ├── config
    ├── filter
    └── exception
```

---

## GitHub 구성

- 레포지토리: https://github.com/PParksung/MOA
- Projects: MOA 개발 보드 (Backlog → Todo → In Progress → In Review → Done)
- Milestones: v0.1 기본기능 / v0.2 거래체결 / v0.3 시세연동 / v1.0 출시
- Labels: feat / fix / docs / refactor / chore / test
- Branch Protection: main, develop 직접 push 불가

---

## 현재 진행 상태

- [x] 프로젝트 기획 및 문제 정의
- [x] GitHub 레포지토리 세팅 (브랜치 보호, 라벨, 마일스톤, 칸반)
- [x] CONTRIBUTING.md 작성 (개발 규칙 문서화)
- [x] Spring Boot 프로젝트 생성 (5개 서버)
- [x] 설계 개선 확정 (BCrypt, AES-GCM, ZSET 오더북, Kafka, WebSocket 푸시, 모니터링)
- [ ] DB 스키마 설계 및 생성 (Flyway)
- [ ] 사용자 인증 구현
- [ ] 현금/증권 계좌 구현
- [ ] KIS API 연동
- [ ] 거래 체결 엔진 구현
- [ ] Kafka 체결 이벤트 연동
- [ ] AI 분석 기능 구현 (패턴 분석 + FDS)
- [ ] React 프론트엔드 구현
- [ ] 실시간 알림 (WebSocket)
- [ ] Docker Compose 배포 환경 구성
- [ ] 모니터링 (Prometheus + Grafana)
- [ ] 부하 테스트 및 성능 최적화 (nGrinder)

---

## 참고 자료
`description for develop/` 폴더 (로컬 전용, gitignore 처리):
- 전체적인 설명.md — 도메인 개념
- 기능명세서.md — API 명세 참고
- 증권사서버기능.md — security-server 상세
- 거래소기능명세서.Md — exchange-server 상세
- 오더북모델문서.md — Redis 오더북 구조 (주의: 가격 목록은 LIST → ZSET으로 설계 변경됨. 이 문서는 구버전)
- ERD.png — DB 설계 참고
- 부하테스트.md, 병목지점탐색후 개선하기.md — 성능 최적화 참고
