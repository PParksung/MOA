# MOA 개발 규칙

## 브랜치 규칙

### 브랜치 전략 (Git Flow)
```
main      ← 배포 가능한 완성 코드만. PR로만 머지 가능.
develop   ← 개발 통합 브랜치. PR로만 머지 가능.
feat/xxx  ← 기능 개발
fix/xxx   ← 버그 수정
docs/xxx  ← 문서 작업
refactor/xxx ← 리팩토링
chore/xxx ← 설정, 빌드
```

### 브랜치 네이밍
```
{타입}/{이슈번호}-{간단한 설명}
```
예시:
```
feat/1-user-signup
fix/12-token-expiry
docs/5-readme-update
refactor/8-order-service
```

### 작업 흐름
```
1. GitHub Issue 생성
2. develop에서 브랜치 생성
3. 개발
4. develop으로 PR 생성
5. 셀프 리뷰 후 머지
6. 완성된 기능 묶음은 develop → main PR
```

---

## 커밋 컨벤션

```
{타입}: {설명}
```

| 타입 | 설명 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `refactor` | 리팩토링 (기능 변화 없음) |
| `chore` | 설정, 빌드, 의존성 변경 |
| `test` | 테스트 추가/수정 |

예시:
```
feat: 회원가입 API 구현
fix: access token 만료 오류 수정
refactor: OrderService 체결 로직 분리
chore: JWT 의존성 추가
```

---

## 포트 규칙

| 서버 | 포트 |
|------|------|
| security-server (증권사 서버) | 8080 |
| exchange-server (거래소 서버) | 8081 |
| frontend (React) | 5173 |
| MySQL | 3306 |
| Redis | 6379 |

---

## PR 규칙

- PR 제목은 커밋 컨벤션과 동일한 형식
- PR은 반드시 관련 Issue 링크
- `develop` 브랜치로만 PR (main 직접 PR 금지)
- 머지 전 셀프 리뷰 필수

---

## 패키지 구조 규칙

```
com.mockinvestment.{server}
├── domain
│   └── {도메인명}
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
├── global
│   ├── config
│   ├── filter
│   └── exception
└── {Server}Application.java
```

---

## 환경변수 규칙

- 민감 정보(API Key, DB 비밀번호 등)는 `.env` 또는 `application-secret.yml`에 저장
- 해당 파일은 `.gitignore` 처리, GitHub에 절대 올리지 않음
- 팀 공유는 별도 채널로 전달
