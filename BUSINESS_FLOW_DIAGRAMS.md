# Auth Service 비즈니스 플로우 다이어그램

## 1. 전체 사용자 여정 맵

```mermaid
journey
    title 사용자 인증/인가 여정
    section 회원가입
      본사 회원가입: 5: 본사
      협력사 생성: 4: 본사, 협력사
      초기 비밀번호 변경: 3: 협력사
    section 로그인
      이메일 로그인: 5: 본사
      계층ID 로그인: 4: 협력사
      JWT 토큰 발급: 5: 본사, 협력사
    section 권한 확인
      데이터 접근 요청: 4: 본사, 협력사
      TreePath 권한 검증: 3: 협력사
      접근 허용/거부: 2: 협력사
    section 관리
      비밀번호 변경: 4: 본사, 협력사
      계정 상태 관리: 5: 본사
      로그아웃: 5: 본사, 협력사
```

## 2. 협력사 온보딩 프로세스

```mermaid
sequenceDiagram
    participant Admin as 본사 관리자
    participant System as Auth System
    participant DART as DART API
    participant NewPartner as 신규 협력사
    participant Email as 이메일 시스템
    
    Admin->>DART: 협력사 정보 조회 요청
    DART-->>Admin: 회사 정보 반환 (UUID, 회사명, 주소 등)
    
    Admin->>System: 협력사 생성 요청
    Note over Admin,System: UUID, 상위협력사 정보 포함
    
    System->>System: 계층적 ID 생성 (L1-001)
    System->>System: TreePath 생성 (/1/L1-001/)
    System->>System: 초기 비밀번호 생성 (hierarchicalId와 동일)
    
    System-->>Admin: 협력사 생성 완료
    Note over System,Admin: 계정 정보: 2412161700-L1-001
    
    Admin->>Email: 계정 정보 전달
    Email->>NewPartner: 로그인 정보 안내
    Note over Email,NewPartner: 계정: 2412161700-L1-001<br/>초기 비밀번호: L1-001
    
    NewPartner->>System: 초기 로그인 시도
    System->>System: 초기 비밀번호 검증
    System-->>NewPartner: 비밀번호 변경 요구
    
    NewPartner->>System: 새 비밀번호 설정
    System->>System: BCrypt 암호화 저장
    System->>System: password_changed = true
    System-->>NewPartner: 온보딩 완료
```

## 3. 멀티레벨 권한 검증 플로우

```mermaid
flowchart TD
    START([API 요청: /partners/123/data]) --> AUTH_CHECK{인증 확인}
    
    AUTH_CHECK -->|미인증| UNAUTHORIZED[401 Unauthorized]
    AUTH_CHECK -->|인증됨| USER_TYPE{사용자 타입}
    
    USER_TYPE -->|HEADQUARTERS| HQ_LOGIC[본사 로직]
    USER_TYPE -->|PARTNER| PARTNER_LOGIC[협력사 로직]
    
    HQ_LOGIC --> HQ_FULL[전체 데이터 접근 허용]
    
    PARTNER_LOGIC --> GET_CURRENT_PATH[현재 사용자 TreePath 조회]
    GET_CURRENT_PATH --> GET_TARGET_PATH[대상 리소스 TreePath 조회]
    
    GET_TARGET_PATH --> PATH_COMPARE{경로 비교}
    
    PATH_COMPARE -->|target = current| SELF_DATA[본인 데이터 접근]
    PATH_COMPARE -->|target starts with current| CHILD_DATA[하위 조직 데이터 접근]
    PATH_COMPARE -->|기타| ACCESS_DENIED[접근 거부]
    
    SELF_DATA --> ALLOWED[접근 허용]
    CHILD_DATA --> LEVEL_CHECK{레벨 차이 확인}
    
    LEVEL_CHECK -->|1레벨 차이| DIRECT_CHILD[직속 하위 접근 허용]
    LEVEL_CHECK -->|2레벨 이상| ALL_DESCENDANTS[모든 하위 접근 허용]
    
    DIRECT_CHILD --> ALLOWED
    ALL_DESCENDANTS --> ALLOWED
    
    HQ_FULL --> SUCCESS[200 OK + 데이터]
    ALLOWED --> SUCCESS
    ACCESS_DENIED --> FORBIDDEN[403 Forbidden]
    UNAUTHORIZED --> END([응답 반환])
    FORBIDDEN --> END
    SUCCESS --> END
    
    style START fill:#e8f5e8
    style SUCCESS fill:#e8f5e8
    style UNAUTHORIZED fill:#ffebee
    style FORBIDDEN fill:#ffebee
    style HQ_FULL fill:#e3f2fd
    style ALLOWED fill:#f1f8e9
```

## 4. JWT 토큰 갱신 플로우

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant API as Auth API
    participant JWT as JWT Utils
    participant DB as Database
    participant Cookie as HTTP Cookie
    
    Note over C: Access Token 만료 (15분)
    
    C->>API: API 요청 (만료된 토큰)
    API->>JWT: validateToken(expiredToken)
    JWT-->>API: TokenExpiredException
    
    API-->>C: 401 Unauthorized + 토큰 만료 메시지
    
    C->>API: POST /refresh-token
    Note over C,API: Refresh Token in Body
    
    API->>JWT: validateRefreshToken(refreshToken)
    
    alt 유효한 Refresh Token
        JWT-->>API: true
        API->>JWT: getAccountFromRefreshToken()
        JWT-->>API: accountNumber
        
        API->>DB: 계정 정보 조회
        DB-->>API: 사용자 정보
        
        API->>JWT: generateNewAccessToken(claims)
        JWT-->>API: 새로운 Access Token
        
        API->>Cookie: 새 토큰으로 쿠키 갱신
        API-->>C: 200 OK + 새 토큰 정보
        
        Note over C: 원본 API 재요청
        
    else 만료된 Refresh Token
        JWT-->>API: TokenExpiredException
        API-->>C: 401 Unauthorized + 재로그인 요구
    end
```

## 5. 계층별 데이터 접근 패턴

```mermaid
graph TB
    subgraph "데이터 접근 권한 매트릭스"
        subgraph "본사 (HQ)"
            HQ_USER[본사 사용자<br/>TreePath: /1/]
            HQ_ACCESS[접근 가능 범위:<br/>✅ 모든 데이터<br/>✅ 모든 협력사<br/>✅ 시스템 관리]
        end
        
        subgraph "1차 협력사 (L1)"
            L1_USER[1차 협력사<br/>TreePath: /1/L1-001/]
            L1_ACCESS[접근 가능 범위:<br/>✅ 본인 데이터<br/>✅ 2차 협력사 데이터<br/>✅ 3차 협력사 데이터<br/>❌ 다른 1차 협력사]
        end
        
        subgraph "2차 협력사 (L2)"
            L2_USER[2차 협력사<br/>TreePath: /1/L1-001/L2-001/]
            L2_ACCESS[접근 가능 범위:<br/>✅ 본인 데이터<br/>✅ 3차 협력사 데이터<br/>❌ 1차 협력사<br/>❌ 다른 2차 협력사]
        end
        
        subgraph "3차 협력사 (L3)"
            L3_USER[3차 협력사<br/>TreePath: /1/L1-001/L2-001/L3-001/]
            L3_ACCESS[접근 가능 범위:<br/>✅ 본인 데이터만<br/>❌ 모든 상위 조직<br/>❌ 모든 동일레벨]
        end
    end
    
    HQ_USER -.-> L1_USER
    HQ_USER -.-> L2_USER  
    HQ_USER -.-> L3_USER
    L1_USER -.-> L2_USER
    L1_USER -.-> L3_USER
    L2_USER -.-> L3_USER
    
    style HQ_USER fill:#e3f2fd
    style L1_USER fill:#f3e5f5
    style L2_USER fill:#fff3e0
    style L3_USER fill:#f1f8e9
```

## 6. 보안 위협 및 대응 플로우

```mermaid
flowchart TD
    THREATS[보안 위협] --> XSS{XSS 공격}
    THREATS --> CSRF{CSRF 공격}
    THREATS --> JWT_THEFT{JWT 토큰 탈취}
    THREATS --> BRUTE_FORCE{무차별 대입 공격}
    
    XSS -->|대응| HTTP_ONLY[HttpOnly 쿠키<br/>스크립트 접근 차단]
    
    CSRF -->|대응| SAME_SITE[SameSite=Strict<br/>교차 사이트 요청 차단]
    
    JWT_THEFT -->|대응| SHORT_EXPIRE[짧은 토큰 만료시간<br/>15분 Access Token]
    JWT_THEFT -->|대응| HTTPS_ONLY[HTTPS 전용 전송<br/>Secure 플래그]
    
    BRUTE_FORCE -->|대응| BCRYPT[BCrypt 강도 12<br/>느린 해시 연산]
    BRUTE_FORCE -->|대응| RATE_LIMIT[API 요청 제한<br/>Rate Limiting]
    
    HTTP_ONLY --> SECURE_IMPL[보안 구현 완료]
    SAME_SITE --> SECURE_IMPL
    SHORT_EXPIRE --> SECURE_IMPL
    HTTPS_ONLY --> SECURE_IMPL
    BCRYPT --> SECURE_IMPL
    RATE_LIMIT --> SECURE_IMPL
    
    style THREATS fill:#ffcdd2
    style XSS fill:#ffebee
    style CSRF fill:#ffebee
    style JWT_THEFT fill:#ffebee
    style BRUTE_FORCE fill:#ffebee
    style SECURE_IMPL fill:#e8f5e8
```

## 7. 계정 상태 관리 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> PENDING: 계정 생성
    
    PENDING --> ACTIVE: 초기 비밀번호 변경
    PENDING --> INACTIVE: 활성화 기간 만료
    
    ACTIVE --> SUSPENDED: 관리자 정지
    ACTIVE --> INACTIVE: 장기간 미사용
    
    SUSPENDED --> ACTIVE: 관리자 활성화
    SUSPENDED --> INACTIVE: 영구 정지
    
    INACTIVE --> ACTIVE: 관리자 재활성화
    INACTIVE --> [*]: 계정 삭제
    
    note right of PENDING
        새로 생성된 협력사
        초기 비밀번호 상태
    end note
    
    note right of ACTIVE
        정상 사용 중인 계정
        모든 기능 이용 가능
    end note
    
    note right of SUSPENDED
        일시적 정지 상태
        로그인 불가
    end note
    
    note right of INACTIVE
        비활성 상태
        재활성화 필요
    end note
```

## 8. API 응답 표준화 구조

```mermaid
classDiagram
    class ApiResponse~T~ {
        +boolean success
        +T data
        +String message
        +String errorCode
        +LocalDateTime timestamp
        +static success(T data, String message) ApiResponse~T~
        +static error(String message, String errorCode) ApiResponse~String~
    }
    
    class TokenResponse {
        +String accessToken
        +String refreshToken
        +Long expiresIn
        +String accountNumber
        +String companyName
        +String userType
        +Integer level
    }
    
    class HeadquartersResponse {
        +Long headquartersId
        +String uuid
        +String hqAccountNumber
        +String companyName
        +String email
        +String name
        +String status
    }
    
    class PartnerResponse {
        +Long partnerId
        +String uuid
        +String hierarchicalId
        +String companyName
        +Integer level
        +String treePath
        +String status
        +Boolean passwordChanged
    }
    
    class ErrorResponse {
        +String message
        +String errorCode
        +LocalDateTime timestamp
        +String path
    }
    
    ApiResponse --> TokenResponse : contains
    ApiResponse --> HeadquartersResponse : contains
    ApiResponse --> PartnerResponse : contains
    ApiResponse --> ErrorResponse : contains
```

이러한 비즈니스 플로우 다이어그램들을 통해 Auth Service의 실제 운영 시나리오와 사용자 경험을 명확하게 보여줄 수 있습니다!