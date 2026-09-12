# MOA 🌱
> 실제 시세로 연습하는 AI 모의투자 플랫폼

**Mock Onboarding to Assets**

투자가 하고 싶은데 실제 돈을 잃을까봐 망설여진다면,
MOA에서 먼저 경험해보세요.

## 기술 스택
- **Backend**: Java 21, Spring Boot 3.5.5, MySQL 8.4, Redis 7
- **Frontend**: React, TypeScript, Tailwind CSS
- **외부 API**: 한국투자증권 Open API (실시간 시세)

## 서비스 구조
- `gateway` : Spring Cloud Gateway, 라우팅 (port 8082)
- `security-server` : 사용자 인증, 계좌 관리, 주문 접수 (port 8080)
- `exchange-server` : 거래 체결 엔진, 미체결 주문 대기열 — 내부 전용 (port 8081)
- `market-server` : KIS 실시간 시세·호가, 보조지표 (port 8083)
- `ai-server` : 투자 규칙 준수·습관 리포트 (port 8084)
- `frontend` : React 웹 클라이언트 (port 5173)

## 로컬 실행
사전 준비: Java 21, Docker Desktop

```bash
# 1. 환경변수 준비 (.env 는 git 에 올라가지 않는다)
cp .env.example .env
#    JWT_SECRET → openssl rand -base64 48
#    AES_KEY    → openssl rand -base64 32
#    DB_*       → 원하는 값

# 2. MySQL 8.4 + Redis 7 기동 (moa_security DB 자동 생성)
docker compose up -d
docker compose ps        # 둘 다 (healthy) 인지 확인

# 3. 서버 실행 (.env 를 환경변수로 로드한 뒤 bootRun)
set -a; source .env; set +a
cd security-server && ./gradlew bootRun
#    기동 시 Flyway 가 db/migration 의 V1 을 자동 적용한다

# 4. 테스트 (Docker 필요 — Testcontainers 가 MySQL 을 직접 띄우므로 .env 불필요)
./gradlew test
```

로컬 3306 / 6379 / 8080 을 이미 쓰는 프로세스(brew mysql, redis, httpd 등)가 있으면 먼저 내린다:
`brew services stop mysql redis httpd`
