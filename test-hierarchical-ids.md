# 계층적 아이디 시스템 테스트 가이드

## 🧪 테스트 시나리오

### 1. 본사 회원가입 및 로그인

```bash
# 1-1. 본사 회원가입
curl -X POST http://localhost:8080/api/v1/headquarters/signup \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "삼성그룹",
    "email": "admin@samsung.com",
    "password": "Admin123!@#",
    "name": "김관리",
    "department": "경영지원팀",
    "position": "팀장",
    "phone": "02-1234-5678",
    "address": "서울시 서초구"
  }'

# 1-2. 본사 로그인 (토큰 획득)
curl -X POST http://localhost:8080/api/v1/headquarters/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@samsung.com",
    "password": "Admin123!@#"
  }'
```

### 2. 1차 협력사 계정 생성

```bash
# 2-1. 김철수 (1차 협력사)
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {본사_토큰}" \
  -d '{
    "companyName": "삼성전자",
    "email": "kcs@samsung.com",
    "contactPerson": "김철수",
    "phone": "02-1111-1111",
    "address": "경기도 수원시",
    "parentId": null
  }'

# 2-2. 최민호 (1차 협력사)
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {본사_토큰}" \
  -d '{
    "companyName": "삼성SDI",
    "email": "cmh@samsungsdi.com",
    "contactPerson": "최민호",
    "phone": "02-2222-2222",
    "address": "경기도 성남시",
    "parentId": null
  }'
```

### 3. 1차 협력사 로그인 및 2차 협력사 생성

```bash
# 3-1. 김철수(p1-kcs01) 로그인
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "p1-kcs01",
    "password": "Kimcheol2024!"
  }'

# 3-2. 김철수가 이영희(2차 협력사) 생성
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {kcs01_토큰}" \
  -d '{
    "companyName": "LG화학",
    "email": "lyh@lgchem.com",
    "contactPerson": "이영희",
    "phone": "02-3333-3333",
    "address": "서울시 영등포구",
    "parentId": 1
  }'

# 3-3. 김철수가 정동원(2차 협력사) 생성
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {kcs01_토큰}" \
  -d '{
    "companyName": "현대자동차",
    "email": "jdw@hyundai.com",
    "contactPerson": "정동원",
    "phone": "02-4444-4444",
    "address": "서울시 서초구",
    "parentId": 1
  }'
```

### 4. 2차 협력사 로그인 및 3차 협력사 생성

```bash
# 4-1. 이영희(p2-lyh01) 로그인
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "p2-lyh01",
    "password": "Leeyoung2024@"
  }'

# 4-2. 이영희가 박민수(3차 협력사) 생성
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {lyh01_토큰}" \
  -d '{
    "companyName": "포스코",
    "email": "pms@posco.com",
    "contactPerson": "박민수",
    "phone": "02-5555-5555",
    "address": "경북 포항시",
    "parentId": 2
  }'
```

### 5. 3차 협력사 로그인

```bash
# 5-1. 박민수(p3-pms01) 로그인
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "p3-pms01",
    "password": "Parkmin2024#"
  }'
```

## 📊 생성된 계층 구조

### 최종 트리 구조

```
본사 (삼성그룹) - admin@samsung.com
├── 1차 협력사 (김철수) → p1-kcs01
│   ├── 2차 협력사 (이영희) → p2-lyh01
│   │   └── 3차 협력사 (박민수) → p3-pms01
│   └── 2차 협력사 (정동원) → p2-jdw01
└── 1차 협력사 (최민호) → p1-cmh01
```

### 생성된 계정 정보

| 레벨 | 담당자 | 아이디            | 이메일             | 회사명     |
| ---- | ------ | ----------------- | ------------------ | ---------- |
| 본사 | 김관리 | admin@samsung.com | admin@samsung.com  | 삼성그룹   |
| 1차  | 김철수 | p1-kcs01          | kcs@samsung.com    | 삼성전자   |
| 1차  | 최민호 | p1-cmh01          | cmh@samsungsdi.com | 삼성SDI    |
| 2차  | 이영희 | p2-lyh01          | lyh@lgchem.com     | LG화학     |
| 2차  | 정동원 | p2-jdw01          | jdw@hyundai.com    | 현대자동차 |
| 3차  | 박민수 | p3-pms01          | pms@posco.com      | 포스코     |

## 🔍 검증 테스트

### 1. 로그인 테스트

각 계정으로 로그인하여 토큰이 정상 발급되는지 확인:

```bash
# 김철수 로그인
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": "p1-kcs01", "password": "Kimcheol2024!"}'

# 이영희 로그인
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": "p2-lyh01", "password": "Leeyoung2024@"}'

# 박민수 로그인
curl -X POST http://localhost:8080/api/v1/partners/login \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": "p3-pms01", "password": "Parkmin2024#"}'
```

### 2. 권한 테스트

각 협력사가 자신의 하위 협력사만 조회할 수 있는지 확인:

```bash
# 김철수가 자신의 하위 협력사 조회
curl -X GET http://localhost:8080/api/v1/partners/children \
  -H "Authorization: Bearer {kcs01_토큰}"

# 이영희가 자신의 하위 협력사 조회
curl -X GET http://localhost:8080/api/v1/partners/children \
  -H "Authorization: Bearer {lyh01_토큰}"
```

### 3. 이니셜 중복 테스트

같은 이니셜을 가진 담당자 추가:

```bash
# 김찬수 (kcs 이니셜 중복) - p1-kcs02 생성되어야 함
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {본사_토큰}" \
  -d '{
    "companyName": "삼성바이오로직스",
    "email": "kcs2@samsung.com",
    "contactPerson": "김찬수",
    "phone": "02-6666-6666",
    "address": "인천시 연수구",
    "parentId": null
  }'
```

## ✅ 예상 결과

### 성공 케이스

1. **아이디 형식**: 모든 협력사 아이디가 `p{레벨}-{이니셜}{순번}` 형식으로 생성
2. **계층 구조**: treePath가 올바르게 설정되어 권한 제어 가능
3. **이니셜 추출**: 한글 이름에서 영문 이니셜이 정확히 추출
4. **중복 처리**: 같은 이니셜인 경우 순번이 자동 증가

### 확인 포인트

- [ ] 담당자 이름에서 이니셜이 올바르게 추출되는가?
- [ ] 계층 레벨이 아이디에 정확히 반영되는가?
- [ ] 같은 이니셜 중복 시 순번이 자동 증가하는가?
- [ ] 각 레벨별 권한이 올바르게 동작하는가?
- [ ] 로그인이 모든 아이디 형식에서 정상 동작하는가?

## 🐛 문제 해결

### 자주 발생할 수 있는 문제

1. **이니셜 추출 오류**: 한글 이름 매핑이 없는 경우
2. **중복 순번 오류**: 데이터베이스 동시성 문제
3. **권한 오류**: treePath 생성 실패
4. **로그인 실패**: 비밀번호 생성 규칙 불일치

### 로그 확인

```bash
# 서비스 로그 확인
docker logs auth-service -f

# 특정 에러 패턴 검색
docker logs auth-service | grep "ERROR\|Exception"
```
