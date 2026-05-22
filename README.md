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
- `security-server` : 사용자 인증, 계좌 관리, 주문 접수 (port 8080)
- `exchange-server` : 거래 체결 엔진, 오더북 관리 (port 8081)
- `frontend` : React 웹 클라이언트 (port 5173)