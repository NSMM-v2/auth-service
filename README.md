# ESG Auth Service API 문서

ESG 프로젝트의 인증 서비스입니다. 본사와 협력사 간의 계층적 권한 관리를 지원하는 마이크로서비스입니다.

## 🆕 새로운 계층적 ID 시스템 (2024년 업데이트)

### 계층적 ID 체계

- **1차 협력사**: `L1-001`, `L1-002`, `L1-003`...
- **2차 협력사**: `L2-001`, `L2-002`, `L2-003`...
- **3차 협력사**: `L3-001`, `L3-002`, `L3-003`...

### 트리 경로 체계

- **1차 협력사**: `/{본사ID}/L1-001/`
- **2차 협력사**: `/{본사ID}/L1-001/L2-001/`
- **3차 협력사**: `/{본사ID}/L1-001/L2-001/L3-001/`

### 초기 비밀번호

- **규칙**: 계층적 ID와 동일 (예: `L1-001`, `L2-001`)
- **첫 로그인 후**: 반드시 복잡한 비밀번호로 변경 필요

## 📋 목차

- [🖥️ 서버 정보](#️-서버-정보)
- [🔐 인증 방식](#-인증-방식)
- [📡 본사 API](#-본사-api)
- [🏢 협력사 API](#-협력사-api)
- [❌ 에러 코드](#-에러-코드)
- [🧪 테스트 시나리오](#-테스트-시나리오)
- [📝 참고사항](#-참고사항)

## 🖥️ 서버 정보

- **개발 서버**: `http://localhost:8081`
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **API Docs**: `http://localhost:8081/api-docs`

## 🔐 인증 방식

### JWT 토큰 기반 인증

- **Access Token**: 15분 유효 (900초)
- **Refresh Token**: 7일 유효
- **전송 방식**:
  1. **쿠키**: `jwt` (HttpOnly, Secure, SameSite=Strict) - 권장
  2. **헤더**: `Authorization: Bearer {token}`

### 권한 체계

- **본사 (HEADQUARTERS)**: 모든 협력사 데이터 접근 가능
- **협력사 (PARTNER)**: 자신과 하위 협력사만 접근 가능

## 📡 본사 API

### 1. 본사 회원가입

새로운 본사 계정을 생성합니다. 계정 번호는 YYMMDD17XX 형식으로 자동 생성됩니다.

**엔드포인트**

```
POST /api/v1/headquarters/register
```

**요청 본문**

```json
{
  "companyName": "테스트 본사",
  "email": "test@company.com",
  "password": "Test123!@#",
  "name": "홍길동",
  "department": "IT팀",
  "position": "팀장",
  "phone": "010-1234-5678",
  "address": "서울시 강남구 테헤란로 123"
}
```

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "본사 회원가입이 성공적으로 완료되었습니다.",
  "data": {
    "id": 1,
    "hqAccountNumber": "2412161700",
    "companyName": "테스트 본사",
    "email": "test@company.com",
    "name": "홍길동",
    "department": "IT팀",
    "position": "팀장",
    "phone": "010-1234-5678",
    "address": "서울시 강남구 테헤란로 123",
    "status": "ACTIVE",
    "createdAt": "2024-12-16T17:00:00"
  },
  "errorCode": null,
  "timestamp": "2024-12-16T17:00:00"
}
```

**curl 테스트**

```bash
curl -X POST http://localhost:8081/api/v1/headquarters/register \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "테스트 본사",
    "email": "test@company.com",
    "password": "Test123!@#",
    "name": "홍길동",
    "department": "IT팀",
    "position": "팀장",
    "phone": "010-1234-5678",
    "address": "서울시 강남구 테헤란로 123"
  }'
```

### 2. 본사 로그인

**엔드포인트**

```
POST /api/v1/headquarters/login
```

**요청 본문**

```json
{
  "email": "test@company.com",
  "password": "Test123!@#"
}
```

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "로그인이 성공적으로 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900000,
    "issuedAt": "2024-12-16T17:00:00",
    "expiresAt": "2024-12-16T17:15:00",
    "accountNumber": "2412161700",
    "companyName": "테스트 본사",
    "userType": "HEADQUARTERS",
    "level": null
  },
  "errorCode": null,
  "timestamp": "2024-12-16T17:00:00"
}
```

### 3. 로그아웃

**엔드포인트**

```
POST /api/v1/headquarters/logout
```

**요청 헤더**

```
Authorization: Bearer {accessToken}
```

### 4. 이메일 중복 확인

**엔드포인트**

```
GET /api/v1/headquarters/check-email?email={email}
```

### 5. 다음 계정번호 미리 확인

**엔드포인트**

```
GET /api/v1/headquarters/next-account-number
```

### 6. 계정번호 유효성 검증

**엔드포인트**

```
GET /api/v1/headquarters/validate-account-number?accountNumber={accountNumber}
```

## 🏢 협력사 API

### 1. 1차 협력사 생성 (본사에서)

본사에서 1차 협력사를 생성합니다.

**엔드포인트**

```
POST /api/v1/partners/first-level
```

**요청 헤더**

```
Authorization: Bearer {본사_토큰}
Content-Type: application/json
X-Headquarters-Id: 1
```

**요청 본문**

```json
{
  "companyName": "삼성전자",
  "email": "samsung@example.com",
  "contactPerson": "김철수",
  "phone": "02-9876-5432",
  "address": "서울시 서초구"
}
```

**성공 응답 (201 Created)**

```json
{
  "success": true,
  "message": "1차 협력사가 성공적으로 생성되었습니다.",
  "data": {
    "partnerId": 1,
    "hqAccountNumber": "2412161700",
    "hierarchicalId": "L1-001",
    "fullAccountNumber": "2412161700-L1-001",
    "companyName": "삼성전자",
    "contactPerson": "김철수",
    "initialPassword": "L1-001",
    "level": 1,
    "treePath": "/1/L1-001/",
    "createdAt": "2024-12-16T17:30:00",
    "message": "협력사가 성공적으로 생성되었습니다. 초기 비밀번호로 로그인 후 비밀번호를 변경해주세요."
  },
  "errorCode": null,
  "timestamp": "2024-12-16T17:30:00"
}
```

### 2. 하위 협력사 생성 (협력사에서)

상위 협력사에서 하위 협력사를 생성합니다.

**엔드포인트**

```
POST /api/v1/partners/{parentId}/sub-partners
```

**요청 헤더**

```
Authorization: Bearer {협력사_토큰}
Content-Type: application/json
```

**요청 본문**

```json
{
  "companyName": "LG화학",
  "email": "lgchem@example.com",
  "contactPerson": "이영희",
  "phone": "02-1111-2222",
  "address": "서울시 영등포구"
}
```

**성공 응답 (201 Created)**

```json
{
  "success": true,
  "message": "하위 협력사가 성공적으로 생성되었습니다.",
  "data": {
    "partnerId": 2,
    "hqAccountNumber": "2412161700",
    "hierarchicalId": "L2-001",
    "fullAccountNumber": "2412161700-L2-001",
    "companyName": "LG화학",
    "contactPerson": "이영희",
    "initialPassword": "L2-001",
    "level": 2,
    "treePath": "/1/L1-001/L2-001/",
    "createdAt": "2024-12-16T17:35:00"
  },
  "errorCode": null,
  "timestamp": "2024-12-16T17:35:00"
}
```

### 3. 협력사 로그인

계층적 ID 기반 로그인입니다.

**엔드포인트**

```
POST /api/v1/partners/login
```

**요청 본문**

```json
{
  "hqAccountNumber": "2412161700",
  "hierarchicalId": "L1-001",
  "password": "L1-001"
}
```

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "로그인이 성공적으로 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900000,
    "fullAccountNumber": "2412161700-L1-001",
    "companyName": "삼성전자",
    "userType": "PARTNER",
    "level": 1
  },
  "errorCode": null,
  "timestamp": "2024-12-16T17:40:00"
}
```

### 4. 협력사 정보 조회

**엔드포인트**

```
GET /api/v1/partners/{partnerId}
```

**요청 헤더**

```
Authorization: Bearer {토큰}
```

### 5. 1차 협력사 목록 조회

**엔드포인트**

```
GET /api/v1/partners/headquarters/{headquartersId}/first-level
```

### 6. 직접 하위 협력사 목록 조회

**엔드포인트**

```
GET /api/v1/partners/{parentId}/children
```

### 7. 초기 비밀번호 변경

협력사 첫 로그인 후 초기 비밀번호를 변경합니다.

**엔드포인트**

```
PATCH /api/v1/partners/{partnerId}/initial-password
```

**요청 헤더**

```
Authorization: Bearer {협력사_토큰}
Content-Type: application/json
```

**요청 본문**

```json
{
  "newPassword": "NewPassword123!@#",
  "confirmPassword": "NewPassword123!@#"
}
```

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "초기 비밀번호 변경이 완료되었습니다.",
  "data": "비밀번호가 성공적으로 변경되었습니다.",
  "errorCode": null,
  "timestamp": "2024-12-16T18:00:00"
}
```

### 8. 비밀번호 미변경 협력사 목록

**엔드포인트**

```
GET /api/v1/partners/headquarters/{headquartersId}/unchanged-password
```

### 9. 협력사 로그아웃

**엔드포인트**

```
POST /api/v1/partners/logout
```

### 10. 이메일 중복 확인

**엔드포인트**

```
GET /api/v1/partners/check-email?email={email}
```

## ❌ 에러 코드

| 에러 코드               | HTTP 상태 | 설명                         |
| ----------------------- | --------- | ---------------------------- |
| REGISTRATION_FAILED     | 400       | 본사 회원가입 실패           |
| SYSTEM_LIMIT_EXCEEDED   | 400       | 일일 본사 생성 한도 초과     |
| LOGIN_FAILED            | 400       | 로그인 실패 (인증 정보 오류) |
| CREATE_FAILED           | 400       | 협력사 생성 실패             |
| PASSWORD_MISMATCH       | 400       | 비밀번호 확인 불일치         |
| PASSWORD_CHANGE_FAILED  | 400       | 비밀번호 변경 실패           |
| NOT_FOUND               | 404       | 리소스를 찾을 수 없음        |
| AUTHENTICATION_REQUIRED | 401       | 인증이 필요함                |
| ACCESS_DENIED           | 403       | 접근 권한 없음               |
| INTERNAL_ERROR          | 500       | 서버 내부 오류               |

## 🧪 테스트 시나리오

### 시나리오 1: 본사 가입 → 1차 협력사 생성

```bash
# 1. 본사 회원가입
curl -X POST http://localhost:8081/api/v1/headquarters/register \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "테스트 본사",
    "email": "test@company.com",
    "password": "Test123!@#",
    "name": "홍길동",
    "department": "IT팀",
    "position": "팀장",
    "phone": "010-1234-5678",
    "address": "서울시 강남구"
  }'

# 2. 본사 로그인
curl -X POST http://localhost:8081/api/v1/headquarters/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@company.com",
    "password": "Test123!@#"
  }'

# 3. 토큰을 변수에 저장 (응답에서 accessToken 복사)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 4. 1차 협력사 생성
curl -X POST http://localhost:8081/api/v1/partners/first-level \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Headquarters-Id: 1" \
  -d '{
    "companyName": "삼성전자",
    "email": "samsung@example.com",
    "contactPerson": "김철수",
    "phone": "02-9876-5432",
    "address": "서울시 서초구"
  }'
```

### 시나리오 2: 협력사 로그인 → 비밀번호 변경

```bash
# 1. 협력사 로그인 (초기 비밀번호)
curl -X POST http://localhost:8081/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{
    "hqAccountNumber": "2412161700",
    "hierarchicalId": "L1-001",
    "password": "L1-001"
  }'

# 2. 협력사 토큰을 변수에 저장
PARTNER_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 3. 초기 비밀번호 변경
curl -X PATCH http://localhost:8081/api/v1/partners/1/initial-password \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newPassword": "NewPassword123!@#",
    "confirmPassword": "NewPassword123!@#"
  }'
```

### 시나리오 3: 다단계 협력사 생성

```bash
# 1. 1차 협력사에서 2차 협력사 생성
curl -X POST http://localhost:8081/api/v1/partners/1/sub-partners \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "LG화학",
    "email": "lgchem@example.com",
    "contactPerson": "이영희",
    "phone": "02-1111-2222",
    "address": "서울시 영등포구"
  }'

# 2. 2차 협력사 로그인
curl -X POST http://localhost:8081/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{
    "hqAccountNumber": "2412161700",
    "hierarchicalId": "L2-001",
    "password": "L2-001"
  }'
```

## 📝 참고사항

### 계정 번호 체계

#### 본사 계정 번호

- **형식**: `YYMMDD17XX` (10자리)
- **예시**: `2412161700`, `2412161701`
- **규칙**: 날짜(6자리) + 17로 시작하는 순번(4자리)
- **일일 한도**: 100개 (1700~1799)

#### 협력사 계층적 ID

- **형식**: `L{레벨}-{순번}` (3자리 순번)
- **예시**: `L1-001`, `L2-001`, `L3-001`
- **트리 경로**: `/{본사ID}/L{레벨}-{순번}/`

### 권한 관리

#### 본사 권한

- 모든 협력사 생성, 조회, 관리
- 비밀번호 미변경 협력사 모니터링
- 계층별 협력사 통계 조회

#### 협력사 권한

- 자신의 정보 조회/수정
- 직접 하위 협력사 생성
- 하위 협력사 목록 조회

### 보안 특징

#### JWT 토큰

- **쿠키 기반**: HttpOnly, Secure, SameSite=Strict
- **클레임 정보**: 계정번호, 회사명, 권한, 계층 정보
- **자동 갱신**: Refresh Token 활용

#### 비밀번호 정책

- **초기**: 계층적 ID (간단하고 기억하기 쉬움)
- **변경 후**: 8자 이상, 대소문자+숫자+특수문자 포함
- **암호화**: BCrypt 해시

### API 설계 특징

#### RESTful 설계

- 명확한 HTTP 메서드 사용
- 의미있는 URL 구조
- 일관된 응답 형식

#### 상태 관리

- Stateless 설계
- JWT 기반 상태 저장
- 트랜잭션 안전성

### 개발 도구

#### Swagger UI

- **URL**: `http://localhost:8081/swagger-ui.html`
- **기능**: 실시간 API 테스트, 문서화
- **인증**: JWT 쿠키 자동 인식

#### 로깅

- 구조화된 로그 메시지
- 보안 정보 마스킹
- 요청/응답 추적

### 확장성 고려사항

#### 마이크로서비스 아키텍처

- Spring Cloud 기반
- Service Discovery 지원
- Config Server 활용

#### 데이터베이스 최적화

- 인덱스 최적화 (트리 경로, 계층적 ID)
- JPA 관계 매핑 최적화
- 쿼리 성능 튜닝

#### 모니터링

- Actuator 헬스체크
- 메트릭 수집
- 에러 추적
