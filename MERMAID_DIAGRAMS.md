# Auth Service Mermaid 다이어그램 모음

## 1. 전체 시스템 아키텍처 플로우차트

```mermaid
flowchart TB
    subgraph "클라이언트 계층"
        WEB[웹 브라우저]
        MOBILE[모바일 앱]
    end
    
    subgraph "API Gateway 계층"
        GW[API Gateway<br/>포트: 8080]
        LB[Load Balancer]
    end
    
    subgraph "인증 서비스"
        AUTH[Auth Service<br/>포트: 8081]
        JWT_UTIL[JWT Utils]
        SEC_CONFIG[Security Config]
        PWD_UTIL[Password Utils]
    end
    
    subgraph "데이터 계층"
        MYSQL[(MySQL<br/>인증 데이터)]
        REDIS[(Redis<br/>세션 캐시)]
    end
    
    subgraph "외부 서비스"
        EUREKA[Service Registry<br/>포트: 8761]
        CONFIG[Config Server<br/>포트: 8888]
    end
    
    WEB --> LB
    MOBILE --> LB
    LB --> GW
    GW --> AUTH
    
    AUTH --> JWT_UTIL
    AUTH --> SEC_CONFIG
    AUTH --> PWD_UTIL
    
    AUTH --> MYSQL
    AUTH --> REDIS
    
    AUTH --> EUREKA
    AUTH --> CONFIG
    
    style AUTH fill:#e1f5fe
    style MYSQL fill:#fff3e0
    style REDIS fill:#f3e5f5
```

## 2. 협력사 생성 플로우차트

```mermaid
flowchart TD
    START([협력사 생성 요청]) --> CHECK_AUTH{인증된 사용자?}
    
    CHECK_AUTH -->|No| AUTH_ERROR[401 Unauthorized]
    CHECK_AUTH -->|Yes| CHECK_ROLE{사용자 권한 확인}
    
    CHECK_ROLE -->|본사| HQ_FLOW[본사 플로우]
    CHECK_ROLE -->|협력사| PARTNER_FLOW[협력사 플로우]
    CHECK_ROLE -->|권한없음| FORBIDDEN[403 Forbidden]
    
    HQ_FLOW --> VALIDATE_UUID{UUID 유효성 검증}
    PARTNER_FLOW --> CHECK_HIERARCHY{하위 생성 권한 확인}
    
    CHECK_HIERARCHY -->|권한있음| VALIDATE_UUID
    CHECK_HIERARCHY -->|권한없음| FORBIDDEN
    
    VALIDATE_UUID -->|유효하지 않음| UUID_ERROR[400 Bad Request<br/>잘못된 UUID]
    VALIDATE_UUID -->|유효함| CHECK_DUPLICATE{UUID 중복 확인}
    
    CHECK_DUPLICATE -->|중복됨| DUPLICATE_ERROR[400 Bad Request<br/>중복된 UUID]
    CHECK_DUPLICATE -->|중복되지 않음| DETERMINE_LEVEL[계층 레벨 결정]
    
    DETERMINE_LEVEL --> GENERATE_ID[계층적 ID 생성<br/>L1-001, L2-001...]
    GENERATE_ID --> GENERATE_PATH[TreePath 생성<br/>/1/L1-001/]
    GENERATE_PATH --> ENCRYPT_PWD[초기 비밀번호 암호화]
    ENCRYPT_PWD --> SAVE_DB[데이터베이스 저장]
    SAVE_DB --> SUCCESS[201 Created<br/>협력사 생성 완료]
    
    AUTH_ERROR --> END([종료])
    FORBIDDEN --> END
    UUID_ERROR --> END
    DUPLICATE_ERROR --> END
    SUCCESS --> END
    
    style START fill:#e8f5e8
    style SUCCESS fill:#e8f5e8
    style AUTH_ERROR fill:#ffebee
    style FORBIDDEN fill:#ffebee
    style UUID_ERROR fill:#ffebee
    style DUPLICATE_ERROR fill:#ffebee
    style END fill:#f5f5f5
```

## 3. JWT 토큰 검증 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant F as JWT Filter
    participant J as JWT Utils
    participant SC as Security Context
    participant API as API Controller
    participant S as Service Layer
    
    C->>F: HTTP 요청 + JWT 쿠키
    
    F->>F: 쿠키에서 JWT 토큰 추출
    
    alt 토큰이 존재하는 경우
        F->>J: validateToken(token)
        
        alt 유효한 토큰
            J-->>F: true
            F->>J: getClaimsFromToken(token)
            J-->>F: JWT Claims 반환
            
            F->>SC: Authentication 객체 생성 및 설정
            F->>API: 요청 전달
            
            API->>API: @PreAuthorize 권한 검증
            
            alt 권한 있음
                API->>S: 비즈니스 로직 실행
                S-->>API: 결과 반환
                API-->>C: 200 OK + 응답 데이터
            else 권한 없음
                API-->>C: 403 Forbidden
            end
            
        else 무효한 토큰
            J-->>F: false
            F-->>C: 401 Unauthorized
        end
        
    else 토큰이 없는 경우
        F->>API: 요청 전달 (인증 정보 없음)
        API-->>C: 401 Unauthorized
    end
```

## 4. 계층적 권한 검증 플로우차트

```mermaid
flowchart TD
    START([API 요청]) --> EXTRACT_TOKEN[JWT 토큰에서<br/>사용자 정보 추출]
    
    EXTRACT_TOKEN --> GET_USER_TYPE{사용자 타입 확인}
    
    GET_USER_TYPE -->|HEADQUARTERS| HQ_ACCESS[본사: 모든 데이터 접근 허용]
    GET_USER_TYPE -->|PARTNER| GET_TARGET[요청 대상 리소스 확인]
    
    GET_TARGET --> CHECK_TREE_PATH[현재 사용자 TreePath 확인]
    CHECK_TREE_PATH --> COMPARE_PATH{대상 리소스의<br/>TreePath 비교}
    
    COMPARE_PATH -->|본인 데이터| SELF_ACCESS[본인 데이터 접근 허용]
    COMPARE_PATH -->|하위 조직 데이터| CHECK_CHILD{하위 조직 여부 확인}
    COMPARE_PATH -->|상위/동일레벨| ACCESS_DENIED[접근 거부]
    
    CHECK_CHILD -->|하위 조직임| CHILD_ACCESS[하위 조직 데이터 접근 허용]
    CHECK_CHILD -->|하위 조직 아님| ACCESS_DENIED
    
    HQ_ACCESS --> PROCESS_REQUEST[요청 처리]
    SELF_ACCESS --> PROCESS_REQUEST
    CHILD_ACCESS --> PROCESS_REQUEST
    ACCESS_DENIED --> RETURN_403[403 Forbidden 반환]
    
    PROCESS_REQUEST --> SUCCESS[200 OK + 데이터 반환]
    
    style START fill:#e8f5e8
    style SUCCESS fill:#e8f5e8
    style ACCESS_DENIED fill:#ffebee
    style RETURN_403 fill:#ffebee
    style HQ_ACCESS fill:#e3f2fd
    style SELF_ACCESS fill:#fff3e0
    style CHILD_ACCESS fill:#f1f8e9
```

## 5. 비밀번호 변경 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant API as Auth Controller
    participant S as Auth Service
    participant P as Password Utils
    participant D as Database
    participant Log as 감사 로그
    
    C->>API: PUT /change-password
    Note over C,API: { currentPassword, newPassword }
    
    API->>S: changePassword(request)
    
    S->>D: 현재 사용자 정보 조회
    D-->>S: 사용자 엔티티 반환
    
    S->>P: matches(currentPassword, storedPassword)
    P-->>S: 비밀번호 일치 여부
    
    alt 현재 비밀번호 일치
        S->>P: encode(newPassword)
        P-->>S: 암호화된 새 비밀번호
        
        S->>S: 엔티티 불변 업데이트
        S->>D: 업데이트된 엔티티 저장
        D-->>S: 저장 완료
        
        S->>Log: 비밀번호 변경 로그 기록
        
        S-->>API: 변경 성공
        API-->>C: 200 OK
        
    else 현재 비밀번호 불일치
        S-->>API: 인증 실패
        API-->>C: 400 Bad Request
        Note over API,C: "현재 비밀번호가 올바르지 않습니다"
    end
```

## 6. 협력사 계층 구조 조회 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant API as Partner Controller
    participant S as Partner Tree Service
    participant R as Partner Repository
    participant D as Database
    
    C->>API: GET /partners/tree
    Note over C,API: JWT 토큰으로 인증
    
    API->>S: getPartnerTree(userInfo)
    
    S->>S: 사용자 권한 확인
    
    alt 본사 사용자
        S->>R: findAllPartnersByHeadquarters(hqId)
        R->>D: SELECT * FROM partners WHERE headquarters_id = ?
        D-->>R: 모든 협력사 데이터
        R-->>S: 전체 협력사 목록
        
    else 협력사 사용자
        S->>R: findChildPartnersByTreePath(treePath)
        R->>D: SELECT * FROM partners WHERE tree_path LIKE 'treePath%'
        D-->>R: 하위 협력사 데이터
        R-->>S: 하위 협력사 목록
    end
    
    S->>S: 계층 구조 트리 생성
    Note over S: TreePath 기반으로 부모-자식 관계 구성
    
    S-->>API: 계층 구조 응답
    API-->>C: 200 OK + 트리 데이터
    
    Note over C,API: { "headquarters": {...}, "children": [...] }
```

## 7. 에러 처리 플로우차트

```mermaid
flowchart TD
    REQUEST[API 요청] --> TRY_PROCESS{요청 처리 시도}
    
    TRY_PROCESS -->|성공| SUCCESS[200/201 성공 응답]
    TRY_PROCESS -->|예외 발생| CATCH_EXCEPTION[예외 캐치]
    
    CATCH_EXCEPTION --> EXCEPTION_TYPE{예외 타입 확인}
    
    EXCEPTION_TYPE -->|JwtException| JWT_ERROR[401 Unauthorized<br/>토큰 오류]
    EXCEPTION_TYPE -->|AccessDeniedException| ACCESS_ERROR[403 Forbidden<br/>권한 오류]
    EXCEPTION_TYPE -->|ValidationException| VALID_ERROR[400 Bad Request<br/>입력값 오류]
    EXCEPTION_TYPE -->|DuplicateException| DUPLICATE_ERROR[409 Conflict<br/>중복 오류]
    EXCEPTION_TYPE -->|NotFoundException| NOT_FOUND[404 Not Found<br/>리소스 없음]
    EXCEPTION_TYPE -->|기타 예외| INTERNAL_ERROR[500 Internal Error<br/>서버 오류]
    
    JWT_ERROR --> LOG_ERROR[에러 로그 기록]
    ACCESS_ERROR --> LOG_ERROR
    VALID_ERROR --> LOG_ERROR
    DUPLICATE_ERROR --> LOG_ERROR
    NOT_FOUND --> LOG_ERROR
    INTERNAL_ERROR --> LOG_CRITICAL[치명적 오류 로그]
    
    LOG_ERROR --> RETURN_ERROR[표준화된 에러 응답]
    LOG_CRITICAL --> RETURN_ERROR
    
    RETURN_ERROR --> CLIENT_RESPONSE[클라이언트에게 응답]
    SUCCESS --> CLIENT_RESPONSE
    
    style REQUEST fill:#e8f5e8
    style SUCCESS fill:#e8f5e8
    style JWT_ERROR fill:#ffebee
    style ACCESS_ERROR fill:#ffebee
    style VALID_ERROR fill:#fff3e0
    style DUPLICATE_ERROR fill:#fff3e0
    style NOT_FOUND fill:#f3e5f5
    style INTERNAL_ERROR fill:#ffcdd2
```

## 8. 데이터베이스 연관관계 ERD

```mermaid
erDiagram
    headquarters {
        bigint headquarters_id PK "본사 고유 ID"
        varchar headquarters_uuid UK "외부 API용 UUID"
        varchar hq_account_number UK "본사 계정번호"
        varchar company_name "회사명"
        varchar email UK "로그인 이메일"
        varchar password "암호화된 비밀번호"
        varchar name "담당자명"
        varchar department "부서"
        varchar position "직급"
        varchar phone "연락처"
        text address "주소"
        enum status "회사상태(ACTIVE/INACTIVE/SUSPENDED)"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
    }
    
    partners {
        bigint partner_id PK "협력사 고유 ID"
        varchar partner_uuid UK "DART API UUID"
        bigint headquarters_id FK "소속 본사 ID"
        bigint parent_partner_id FK "상위 협력사 ID"
        varchar hq_account_number "본사 계정번호"
        varchar hierarchical_id "계층적 ID(L1-001)"
        varchar company_name "협력사명"
        varchar password "암호화된 비밀번호"
        int level "계층 레벨(1,2,3...)"
        varchar tree_path "트리경로(/1/L1-001/)"
        enum status "협력사상태"
        boolean password_changed "비밀번호 변경여부"
        datetime created_at "생성일시"
        datetime updated_at "수정일시"
    }
    
    headquarters ||--o{ partners : "manages"
    partners ||--o{ partners : "parent-child"
```

## 9. JWT 토큰 구조 다이어그램

```mermaid
graph LR
    subgraph "JWT 토큰 구조"
        subgraph "Header"
            ALG[alg: HS512]
            TYP[typ: JWT]
        end
        
        subgraph "Payload"
            SUB[sub: accountNumber]
            ACC[accountNumber]
            COMP[companyName]
            USER[userType: HQ/PARTNER]
            LVL[level: 1,2,3...]
            TREE[treePath: /1/L1-001/]
            HQID[headquartersId]
            PID[partnerId]
            IAT[iat: 발급시간]
            EXP[exp: 만료시간]
        end
        
        subgraph "Signature"
            SIGN[HMACSHA512 서명]
        end
    end
    
    subgraph "쿠키 설정"
        COOKIE[jwt=token]
        HTTP[HttpOnly: true]
        SECURE[Secure: true]
        SAME[SameSite: Strict]
    end
    
    Header --> SIGN
    Payload --> SIGN
    SIGN --> COOKIE
    
    style Header fill:#e3f2fd
    style Payload fill:#f3e5f5
    style Signature fill:#fff3e0
    style COOKIE fill:#e8f5e8
```

이러한 다이어그램들을 통해 Auth Service의 복잡한 인증/인가 로직과 계층적 구조를 시각적으로 명확하게 설명할 수 있습니다!