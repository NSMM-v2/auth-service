# Auth Service API 문서

ESG 프로젝트의 인증 서비스 API 문서입니다. 본사 회원가입, 로그인, 협력사 계층적 관리 기능을 제공합니다.

## 🆕 새로운 기능: 담당자 이니셜 기반 계층적 아이디 시스템

협력사 아이디가 담당자 이름 기반 계층적 구조로 변경되었습니다:

### 아이디 형식

- **1차 협력사**: `p1-kcs01` (김철수)
- **2차 협력사**: `p2-lyh01` (이영희)
- **3차 협력사**: `p3-pms01` (박민수)

### 특징

- 담당자 이름에서 이니셜 자동 추출
- 레벨별 접두사로 계층 구조 명확히 표현
- 소문자 사용으로 친근하고 현대적
- 직관적이고 기억하기 쉬움

### 이니셜 추출 규칙

| 담당자 이름 | 추출 이니셜 | 생성 아이디 예시 |
| ----------- | ----------- | ---------------- |
| 김철수      | kcs         | p1-kcs01         |
| 이영희      | lyh         | p2-lyh01         |
| 박민수      | pms         | p3-pms01         |
| John Smith  | js          | p1-js01          |

## 📋 목차

- [Auth Service API 문서](#auth-service-api-문서)
  - [📋 목차](#-목차)
  - [🖥️ 서버 정보](#️-서버-정보)
  - [🔐 인증 방식](#-인증-방식)
    - [JWT 토큰 기반 인증](#jwt-토큰-기반-인증)
    - [권한 체계](#권한-체계)
  - [📡 API 엔드포인트](#-api-엔드포인트)
    - [1. 본사 회원가입](#1-본사-회원가입)
    - [2. 본사 로그인](#2-본사-로그인)
    - [3. 본사 정보 조회](#3-본사-정보-조회)
    - [4. 본사 정보 수정](#4-본사-정보-수정)
    - [5. 이메일 중복 확인](#5-이메일-중복-확인)
    - [6. 본사 상태 변경](#6-본사-상태-변경)
    - [7. 로그아웃](#7-로그아웃)
  - [❌ 에러 코드](#-에러-코드)
  - [🧪 테스트 시나리오](#-테스트-시나리오)
    - [시나리오 1: 본사 회원가입 → 로그인 → 정보 조회](#시나리오-1-본사-회원가입--로그인--정보-조회)
    - [시나리오 2: 정보 수정 → 로그아웃](#시나리오-2-정보-수정--로그아웃)
    - [시나리오 3: 이메일 중복 확인](#시나리오-3-이메일-중복-확인)
  - [📝 참고사항](#-참고사항)
    - [JWT 토큰 사용법](#jwt-토큰-사용법)
    - [보안 고려사항](#보안-고려사항)
    - [개발 도구](#개발-도구)

## 🖥️ 서버 정보

- **개발 서버**: `http://localhost:8081`
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **API Docs**: `http://localhost:8081/api-docs`

## 🔐 인증 방식

### JWT 토큰 기반 인증

- **Access Token**: 15분 유효 (900초)
- **토큰 타입**: Bearer
- **전송 방식**:
  1. **쿠키**: `jwt` (HttpOnly, Secure, SameSite=Strict)
  2. **헤더**: `Authorization: Bearer {token}`

### 권한 체계

- **본사 (HEADQUARTERS)**: 모든 협력사 데이터 접근 가능
- **협력사 (PARTNER)**: 자신과 하위 협력사만 접근 가능

## 📡 API 엔드포인트

### 1. 본사 회원가입

새로운 본사 계정을 생성합니다. 계정 번호는 HQ001, HQ002 형식으로 자동 생성됩니다.

**엔드포인트**

```
POST /api/v1/headquarters/signup
```

**요청 헤더**

```
Content-Type: application/json
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

**필드 설명**
| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| companyName | String | ✅ | 최대 255자 | 회사명 |
| email | String | ✅ | 이메일 형식, 고유값 | 로그인 ID |
| password | String | ✅ | 8-100자, 복잡성 규칙 | 비밀번호 |
| name | String | ✅ | 최대 100자 | 담당자명 |
| department | String | ❌ | 최대 100자 | 부서명 |
| position | String | ❌ | 최대 50자 | 직급 |
| phone | String | ❌ | 최대 20자 | 연락처 |
| address | String | ❌ | - | 주소 |

**비밀번호 규칙**

- 최소 8자 이상
- 대문자, 소문자, 숫자, 특수문자 각각 최소 1개 포함

**성공 응답 (201 Created)**

```json
{
  "success": true,
  "message": "본사 회원가입이 완료되었습니다.",
  "data": {
    "id": 1,
    "accountNumber": "HQ001",
    "companyName": "테스트 본사",
    "email": "test@company.com",
    "name": "홍길동",
    "department": "IT팀",
    "position": "팀장",
    "phone": "010-1234-5678",
    "address": "서울시 강남구 테헤란로 123",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": null
  },
  "errorCode": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**실패 응답 (400 Bad Request)**

```json
{
  "success": false,
  "message": "이미 사용 중인 이메일입니다.",
  "data": null,
  "errorCode": "SIGNUP_FAILED",
  "timestamp": "2024-01-01T00:00:00"
}
```

**테스트 데이터**

```bash
curl -X POST http://localhost:8081/api/v1/headquarters/signup \
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

---

### 2. 본사 로그인

본사 계정으로 로그인하여 JWT 토큰을 발급받습니다.

**엔드포인트**

```
POST /api/v1/headquarters/login
```

**요청 헤더**

```
Content-Type: application/json
```

**요청 본문**

```json
{
  "email": "test@company.com",
  "password": "Test123!@#"
}
```

**필드 설명**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✅ | 로그인 이메일 |
| password | String | ✅ | 비밀번호 |

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 900,
    "issuedAt": "2024-01-01T00:00:00",
    "expiresAt": "2024-01-01T00:15:00",
    "accountNumber": "HQ001",
    "companyName": "테스트 본사",
    "userType": "HEADQUARTERS",
    "level": null
  },
  "errorCode": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**쿠키 설정**

- 이름: `jwt`
- 값: JWT 토큰
- 속성: HttpOnly, Secure, SameSite=Strict
- 만료: 24시간

**실패 응답 (401 Unauthorized)**

```json
{
  "success": false,
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "data": null,
  "errorCode": "LOGIN_FAILED",
  "timestamp": "2024-01-01T00:00:00"
}
```

**테스트 데이터**

```bash
curl -X POST http://localhost:8081/api/v1/headquarters/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@company.com",
    "password": "Test123!@#"
  }'
```

---

### 3. 본사 정보 조회

본사 ID로 본사 정보를 조회합니다. 본인의 정보만 조회 가능합니다.

**엔드포인트**

```
GET /api/v1/headquarters/{headquartersId}
```

**요청 헤더**

```
Authorization: Bearer {accessToken}
# 또는 쿠키로 자동 전송
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| headquartersId | Long | ✅ | 본사 ID |

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "accountNumber": "HQ001",
    "companyName": "테스트 본사",
    "email": "test@company.com",
    "name": "홍길동",
    "department": "IT팀",
    "position": "팀장",
    "phone": "010-1234-5678",
    "address": "서울시 강남구 테헤란로 123",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": null
  },
  "errorCode": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**실패 응답**

- **401 Unauthorized**: 인증 실패
- **403 Forbidden**: 권한 없음 (다른 본사 정보 조회 시도)
- **404 Not Found**: 본사 정보를 찾을 수 없음

**테스트 데이터**

```bash
# 로그인 후 토큰 획득
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8081/api/v1/headquarters/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4. 본사 정보 수정

본사 정보를 수정합니다. 본인의 정보만 수정 가능하며, 이메일과 비밀번호는 별도 API를 사용해야 합니다.

**엔드포인트**

```
PUT /api/v1/headquarters/{headquartersId}
```

**요청 헤더**

```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| headquartersId | Long | ✅ | 본사 ID |

**요청 본문**

```json
{
  "companyName": "수정된 본사명",
  "name": "홍길동",
  "department": "개발팀",
  "position": "부장",
  "phone": "010-9876-5432",
  "address": "서울시 서초구 강남대로 456"
}
```

**필드 설명**
| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| companyName | String | ❌ | 최대 255자 | 회사명 (null이면 기존값 유지) |
| name | String | ❌ | 최대 100자 | 담당자명 |
| department | String | ❌ | 최대 100자 | 부서명 |
| position | String | ❌ | 최대 50자 | 직급 |
| phone | String | ❌ | 최대 20자 | 연락처 |
| address | String | ❌ | - | 주소 |

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "본사 정보가 수정되었습니다.",
  "data": {
    "id": 1,
    "accountNumber": "HQ001",
    "companyName": "수정된 본사명",
    "email": "test@company.com",
    "name": "홍길동",
    "department": "개발팀",
    "position": "부장",
    "phone": "010-9876-5432",
    "address": "서울시 서초구 강남대로 456",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T01:00:00"
  },
  "errorCode": null,
  "timestamp": "2024-01-01T01:00:00"
}
```

**테스트 데이터**

```bash
curl -X PUT http://localhost:8081/api/v1/headquarters/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "수정된 본사명",
    "name": "홍길동",
    "department": "개발팀",
    "position": "부장",
    "phone": "010-9876-5432",
    "address": "서울시 서초구 강남대로 456"
  }'
```

---

### 5. 이메일 중복 확인

회원가입 시 이메일 중복 여부를 확인합니다.

**엔드포인트**

```
GET /api/v1/headquarters/check-email?email={email}
```

**쿼리 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| email | String | ✅ | 확인할 이메일 주소 |

**성공 응답 (200 OK)**

**사용 가능한 이메일**

```json
{
  "success": true,
  "message": "사용 가능한 이메일입니다.",
  "data": false,
  "errorCode": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**이미 사용 중인 이메일**

```json
{
  "success": true,
  "message": "이미 사용 중인 이메일입니다.",
  "data": true,
  "errorCode": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**테스트 데이터**

```bash
# 사용 가능한 이메일 확인
curl -X GET "http://localhost:8081/api/v1/headquarters/check-email?email=new@company.com"

# 이미 사용 중인 이메일 확인
curl -X GET "http://localhost:8081/api/v1/headquarters/check-email?email=test@company.com"
```

---

### 6. 본사 상태 변경

본사 계정의 상태를 변경합니다. (ACTIVE, INACTIVE, SUSPENDED)

**엔드포인트**

```
PATCH /api/v1/headquarters/{headquartersId}/status?status={status}
```

**요청 헤더**

```
Authorization: Bearer {accessToken}
```

**경로 매개변수**
| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| headquartersId | Long | ✅ | 본사 ID |

**쿼리 매개변수**
| 매개변수 | 타입 | 필수 | 가능한 값 | 설명 |
|----------|------|------|-----------|------|
| status | String | ✅ | ACTIVE, INACTIVE, SUSPENDED | 변경할 상태 |

**상태 설명**

- **ACTIVE**: 활성 상태
- **INACTIVE**: 비활성 상태
- **SUSPENDED**: 정지 상태

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "본사 상태가 변경되었습니다.",
  "data": null,
  "errorCode": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**실패 응답 (400 Bad Request)**

```json
{
  "success": false,
  "message": "잘못된 상태 값입니다.",
  "data": null,
  "errorCode": "INVALID_STATUS",
  "timestamp": "2024-01-01T00:00:00"
}
```

**테스트 데이터**

```bash
# 상태를 SUSPENDED로 변경
curl -X PATCH "http://localhost:8081/api/v1/headquarters/1/status?status=SUSPENDED" \
  -H "Authorization: Bearer $TOKEN"

# 상태를 ACTIVE로 변경
curl -X PATCH "http://localhost:8081/api/v1/headquarters/1/status?status=ACTIVE" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 7. 로그아웃

현재 세션을 종료하고 JWT 쿠키를 삭제합니다.

**엔드포인트**

```
POST /api/v1/headquarters/logout
```

**요청 헤더**

```
Authorization: Bearer {accessToken}
```

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "message": "로그아웃이 완료되었습니다.",
  "data": null,
  "errorCode": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**쿠키 삭제**

- `jwt` 쿠키가 즉시 만료됨 (MaxAge=0)

**테스트 데이터**

```bash
curl -X POST http://localhost:8081/api/v1/headquarters/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## ❌ 에러 코드

| 에러 코드      | HTTP 상태 | 설명                                          |
| -------------- | --------- | --------------------------------------------- |
| SIGNUP_FAILED  | 400       | 회원가입 실패 (이메일 중복, 유효성 검증 실패) |
| LOGIN_FAILED   | 401       | 로그인 실패 (이메일 또는 비밀번호 오류)       |
| INVALID_STATUS | 400       | 잘못된 상태 값                                |
| NOT_FOUND      | 404       | 리소스를 찾을 수 없음                         |
| INTERNAL_ERROR | 500       | 서버 내부 오류                                |

---

## 🧪 테스트 시나리오

### 시나리오 1: 본사 회원가입 → 로그인 → 정보 조회

```bash
# 1. 회원가입
curl -X POST http://localhost:8081/api/v1/headquarters/signup \
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

# 2. 로그인
curl -X POST http://localhost:8081/api/v1/headquarters/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@company.com",
    "password": "Test123!@#"
  }'

# 3. 토큰을 변수에 저장 (응답에서 accessToken 복사)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 4. 정보 조회
curl -X GET http://localhost:8081/api/v1/headquarters/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 시나리오 2: 정보 수정 → 로그아웃

```bash
# 1. 정보 수정
curl -X PUT http://localhost:8081/api/v1/headquarters/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "수정된 본사명",
    "department": "개발팀",
    "position": "부장"
  }'

# 2. 로그아웃
curl -X POST http://localhost:8081/api/v1/headquarters/logout \
  -H "Authorization: Bearer $TOKEN"
```

### 시나리오 3: 이메일 중복 확인

```bash
# 1. 사용 가능한 이메일 확인
curl -X GET "http://localhost:8081/api/v1/headquarters/check-email?email=new@company.com"

# 2. 이미 사용 중인 이메일 확인
curl -X GET "http://localhost:8081/api/v1/headquarters/check-email?email=test@company.com"
```

---

## 📝 참고사항

### JWT 토큰 사용법

1. **쿠키 방식**: 로그인 시 자동으로 설정되며, 브라우저에서 자동 전송
2. **헤더 방식**: `Authorization: Bearer {token}` 형식으로 수동 전송

### 보안 고려사항

- 모든 비밀번호는 BCrypt로 암호화
- JWT 쿠키는 HttpOnly, Secure, SameSite=Strict 설정
- CORS 정책 적용
- 입력값 유효성 검증

### 개발 도구

- **Swagger UI**: 브라우저에서 직접 API 테스트 가능
- **Postman**: Collection 파일 제공 예정
- **curl**: 명령줄에서 간단한 테스트 가능

# ESG Auth Service

ESG 프로젝트의 인증 서비스입니다. 본사와 협력사 간의 계층적 권한 관리를 지원합니다.

## 새로운 계정 시스템 (2024년 업데이트)

### 개선된 특징

1. **8자리 숫자 계정 번호**: 기존 `HQ001` 대신 `90541842` 같은 직관적인 숫자 형태
2. **회사명 기반 로그인 ID**: `samsung_electronics_2024`, `lg_chem_2024` 같은 의미있는 ID
3. **친화적 비밀번호**: 기억하기 쉬운 단어 조합 (`Samsung2024!`, `Cherry456@`)
4. **다중 로그인 방식**: 숫자 계정, 회사명 ID, 기존 이메일 모두 지원

### 본사 계정 시스템

#### 계정 번호 형태

- **범위**: 10000001 ~ 19999999 (본사는 1로 시작)
- **예시**: `10000001`, `10000002`, `10000003`

#### 로그인 방식

- **이메일**: `admin@samsung.com`
- **비밀번호**: 자동 생성된 친화적 비밀번호 (`Samsung2024!`)

### 협력사 계정 시스템

#### 계정 번호 형태

- **범위**: 20000001 ~ 99999999 (8자리 숫자)
- **예시**: `90541842`, `85673921`, `76543210`

#### 로그인 ID 형태

- **회사명 기반**: `{영문회사명}_{연도}`
- **예시**: `samsung_electronics_2024`, `lg_chem_2024`, `hyundai_motor_2024`

#### 비밀번호 형태

- **회사명 기반**: `{회사명}{연도}{특수문자}`
- **단어 조합**: `{영문단어}{숫자}{특수문자}`
- **예시**: `Samsung2024!`, `Apple123@`, `Seoul456#`

## API 사용 예시

### 1. 본사 회원가입

```bash
curl -X POST http://localhost:8080/api/v1/headquarters/signup \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "삼성전자",
    "email": "admin@samsung.com",
    "name": "김담당",
    "department": "IT사업부",
    "position": "팀장",
    "phone": "02-1234-5678",
    "address": "서울시 강남구"
  }'
```

**응답 예시:**

```json
{
  "success": true,
  "message": "본사 회원가입이 완료되었습니다.",
  "data": {
    "id": 1,
    "accountNumber": "10000001",
    "companyName": "삼성전자",
    "email": "admin@samsung.com",
    "name": "김담당",
    "temporaryPassword": "Samsung2024!",
    "message": "본사 계정이 성공적으로 생성되었습니다. 임시 비밀번호로 로그인 후 비밀번호를 변경해주세요."
  }
}
```

### 2. 본사 로그인

```bash
curl -X POST http://localhost:8080/api/v1/headquarters/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@samsung.com",
    "password": "Samsung2024!"
  }'
```

**응답 예시:**

```json
{
  "success": true,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 900000,
    "accountNumber": "10000001",
    "companyName": "삼성전자",
    "userType": "HEADQUARTERS"
  }
}
```

### 3. 협력사 계정 생성 (새로운 계층적 아이디)

```bash
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {본사_토큰}" \
  -d '{
    "companyName": "삼성전자",
    "email": "kcs@samsung.com",
    "contactPerson": "김철수",
    "phone": "02-9876-5432",
    "address": "서울시 서초구",
    "parentId": null
  }'
```

**응답 예시:**

```json
{
  "success": true,
  "message": "협력사 계정이 성공적으로 생성되었습니다.",
  "data": {
    "id": 1,
    "accountNumber": "p1-kcs01",
    "companyName": "삼성전자",
    "email": "kcs@samsung.com",
    "contactPerson": "김철수",
    "temporaryPassword": "Kimcheol2024!",
    "level": 1,
    "treePath": "/1/",
    "status": "ACTIVE",
    "message": "협력사 계정이 성공적으로 생성되었습니다. 임시 비밀번호로 로그인 후 비밀번호를 변경해주세요."
  }
}
```

### 3-1. 2차 협력사 생성 (1차 협력사에서)

```bash
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {1차_협력사_토큰}" \
  -d '{
    "companyName": "LG화학",
    "email": "lyh@lgchem.com",
    "contactPerson": "이영희",
    "phone": "02-1111-2222",
    "address": "서울시 영등포구",
    "parentId": 1
  }'
```

**응답 예시:**

```json
{
  "success": true,
  "message": "협력사 계정이 성공적으로 생성되었습니다.",
  "data": {
    "id": 2,
    "accountNumber": "p2-lyh01",
    "companyName": "LG화학",
    "email": "lyh@lgchem.com",
    "contactPerson": "이영희",
    "temporaryPassword": "Leeyoung2024@",
    "level": 2,
    "treePath": "/1/2/",
    "status": "ACTIVE"
  }
}
```

### 4. 협력사 로그인 (계층적 아이디)

#### 4-1. 계층적 아이디로 로그인 (추천)

```bash
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "p1-kcs01",
    "password": "Kimcheol2024!"
  }'
```

#### 4-2. 2차 협력사 로그인

```bash
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "p2-lyh01",
    "password": "Leeyoung2024@"
  }'
```

**응답 예시:**

```json
{
  "success": true,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 900000,
    "accountNumber": "p1-kcs01",
    "companyName": "삼성전자",
    "userType": "PARTNER",
    "level": 1
  }
}
```

### 4-3. 트리 구조 예시

로그인 후 다음과 같은 계층 구조를 확인할 수 있습니다:

```
본사 (삼성그룹)
├── 1차 협력사 (김철수) → p1-kcs01
│   ├── 2차 협력사 (이영희) → p2-lyh01
│   │   └── 3차 협력사 (박민수) → p3-pms01
│   └── 2차 협력사 (정동원) → p2-jdw01
└── 1차 협력사 (최민호) → p1-cmh01
    └── 2차 협력사 (김영수) → p2-kys01
```

## 계층적 아이디 생성 규칙

### 담당자 이름 → 이니셜 변환 예시

| 담당자 이름 | 추출 이니셜 | 레벨 | 생성 아이디 | 설명                                    |
| ----------- | ----------- | ---- | ----------- | --------------------------------------- |
| 김철수      | kcs         | 1차  | `p1-kcs01`  | Partner Level 1 - 김(k)철(c)수(s) 01번  |
| 이영희      | lyh         | 2차  | `p2-lyh01`  | Partner Level 2 - 이(l)영(y)희(h) 01번  |
| 박민수      | pms         | 3차  | `p3-pms01`  | Partner Level 3 - 박(p)민(m)수(s) 01번  |
| 정동원      | jdw         | 2차  | `p2-jdw01`  | Partner Level 2 - 정(j)동(d)원(w) 01번  |
| John Smith  | js          | 1차  | `p1-js01`   | Partner Level 1 - John(j) Smith(s) 01번 |

### 이니셜 중복 처리

같은 이니셜을 가진 담당자가 있는 경우:

| 담당자 이름 | 이니셜 | 생성 아이디 | 순번                  |
| ----------- | ------ | ----------- | --------------------- |
| 김철수      | kcs    | `p1-kcs01`  | 첫 번째               |
| 김찬수      | kcs    | `p1-kcs02`  | 두 번째 (같은 이니셜) |
| 김철민      | kcm    | `p1-kcm01`  | 첫 번째 (다른 이니셜) |

### 비밀번호 생성 패턴

담당자 이름 기반 임시 비밀번호 생성:

1. **한글 이름**: `{이름첫글자대문자}{소문자이름}{연도}{특수문자}`
2. **영문 이름**: `{FirstName}{연도}{특수문자}`

**예시:**

- `Kimcheol2024!` (김철수)
- `Leeyoung2024@` (이영희)
- `Parkmin2024#` (박민수)
- `John2024$` (John Smith)

## 보안 특징

### 다중 인증 방식

- **8자리 숫자**: 간단하고 기억하기 쉬움
- **회사명 ID**: 의미있고 직관적
- **이메일**: 기존 방식 호환성

### 비밀번호 정책

- **복잡성**: 대문자, 소문자, 숫자, 특수문자 포함
- **기억 용이성**: 의미있는 단어 조합
- **회사 연관성**: 회사명 기반 생성

### 쿠키 보안

- **HttpOnly**: XSS 공격 방지
- **Secure**: HTTPS 환경에서만 전송
- **SameSite=Strict**: CSRF 공격 방지

## 기존 호환성

기존 계정 방식도 계속 지원됩니다:

- 본사: 이메일 로그인
- 협력사: `HQ001-L1-001` 형태 계정 번호

새로운 방식과 기존 방식을 동시에 사용할 수 있어 점진적 전환이 가능합니다.
