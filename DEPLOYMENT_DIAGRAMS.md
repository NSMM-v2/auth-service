# Auth Service 배포 및 시스템 구성 다이어그램

## 1. 마이크로서비스 전체 아키텍처

```mermaid
C4Context
    title ESG 프로젝트 마이크로서비스 아키텍처
    
    Person(user, "사용자", "본사/협력사 사용자")
    
    System_Boundary(esg, "ESG 프로젝트") {
        System(frontend, "프론트엔드", "Next.js 15")
        System(gateway, "API Gateway", "Spring Cloud Gateway")
        
        System(auth, "Auth Service", "인증/인가 서비스")
        System(scope, "Scope Service", "탄소배출량 관리")
        System(csddd, "CSDDD Service", "규정 준수 관리")
        System(dart, "DART Service", "기업 데이터 통합")
        
        System(config, "Config Service", "중앙 설정 관리")
        System(discovery, "Discovery Service", "서비스 레지스트리")
    }
    
    SystemDb(mysql, "MySQL", "관계형 데이터베이스")
    SystemDb(redis, "Redis", "캐시 및 세션")
    
    System_Ext(github, "GitHub", "설정 저장소")
    System_Ext(dart_api, "DART API", "한국 기업 데이터")
    
    Rel(user, frontend, "사용")
    Rel(frontend, gateway, "API 호출")
    
    Rel(gateway, auth, "인증 요청")
    Rel(gateway, scope, "배출량 API")
    Rel(gateway, csddd, "규정 준수 API")
    Rel(gateway, dart, "기업 데이터 API")
    
    Rel(auth, mysql, "사용자 데이터")
    Rel(auth, redis, "세션 캐시")
    
    Rel(config, github, "설정 조회")
    Rel_Back(discovery, auth, "서비스 등록")
    Rel_Back(discovery, scope, "서비스 등록")
    
    Rel(dart, dart_api, "기업 정보 조회")
```

## 2. Docker 컨테이너 구성

```mermaid
graph TB
    subgraph "Docker Network: esg-network"
        subgraph "Infrastructure"
            MYSQL[MySQL Container<br/>Port: 3306<br/>Volume: mysql-data]
            REDIS[Redis Container<br/>Port: 6379<br/>Volume: redis-data]
        end
        
        subgraph "Core Services"
            CONFIG[Config Service<br/>Port: 8888<br/>Env: GIT_TOKEN]
            DISCOVERY[Discovery Service<br/>Port: 8761<br/>Health Check]
        end
        
        subgraph "Business Services"
            AUTH[Auth Service<br/>Port: 8081<br/>JWT_SECRET]
            GATEWAY[Gateway Service<br/>Port: 8080<br/>Routes Config]
            SCOPE[Scope Service<br/>Port: 8082]
            CSDDD[CSDDD Service<br/>Port: 8083]
        end
        
        subgraph "Frontend"
            NEXT[Next.js App<br/>Port: 3000<br/>API_BASE_URL]
        end
    end
    
    CONFIG --> MYSQL
    DISCOVERY --> MYSQL
    AUTH --> MYSQL
    AUTH --> REDIS
    
    GATEWAY --> AUTH
    GATEWAY --> SCOPE
    GATEWAY --> CSDDD
    
    NEXT --> GATEWAY
    
    style AUTH fill:#e3f2fd
    style MYSQL fill:#fff3e0
    style REDIS fill:#f3e5f5
```

## 3. 환경별 배포 전략

```mermaid
gitGraph
    commit id: "Initial"
    branch development
    checkout development
    commit id: "Feature A"
    commit id: "Feature B"
    
    checkout main
    merge development
    commit id: "Release v1.0"
    
    branch staging
    checkout staging
    commit id: "Staging Deploy"
    
    branch production
    checkout production
    commit id: "Production Deploy"
    
    checkout development
    commit id: "Feature C"
    commit id: "Hotfix"
    
    checkout main
    merge development
    commit id: "Release v1.1"
    
    checkout staging
    merge main
    commit id: "Staging v1.1"
    
    checkout production
    merge staging
    commit id: "Production v1.1"
```

## 4. 서비스 헬스체크 및 모니터링

```mermaid
sequenceDiagram
    participant LB as Load Balancer
    participant AUTH as Auth Service
    participant EUREKA as Eureka Server
    participant MONITOR as Monitoring
    participant ALERT as Alert System
    
    loop 헬스체크 (30초마다)
        LB->>AUTH: GET /actuator/health
        AUTH-->>LB: 200 OK + Health Status
        
        AUTH->>EUREKA: 서비스 상태 전송
        EUREKA-->>AUTH: 등록 확인
        
        AUTH->>MONITOR: 메트릭 전송
        Note over AUTH,MONITOR: CPU, Memory, Request Count
        
        alt 서비스 정상
            MONITOR-->>AUTH: 정상 상태 확인
        else 서비스 이상
            MONITOR->>ALERT: 알림 발송
            ALERT->>MONITOR: 관리자 통지
        end
    end
```

## 5. 로드 밸런싱 및 확장성

```mermaid
graph TB
    subgraph "External"
        USER[사용자 요청]
        CDN[CDN/CloudFlare]
    end
    
    subgraph "Load Balancer Layer"
        ALB[Application Load Balancer<br/>SSL Termination]
        NLB[Network Load Balancer<br/>Health Check]
    end
    
    subgraph "API Gateway Cluster"
        GW1[Gateway Instance 1<br/>8080]
        GW2[Gateway Instance 2<br/>8080]
        GW3[Gateway Instance 3<br/>8080]
    end
    
    subgraph "Auth Service Cluster"
        AUTH1[Auth Instance 1<br/>8081]
        AUTH2[Auth Instance 2<br/>8081]
        AUTH3[Auth Instance 3<br/>8081]
    end
    
    subgraph "Database Cluster"
        MYSQL_M[MySQL Master<br/>Write Operations]
        MYSQL_R1[MySQL Replica 1<br/>Read Operations]
        MYSQL_R2[MySQL Replica 2<br/>Read Operations]
        REDIS_CLUSTER[Redis Cluster<br/>Session Storage]
    end
    
    USER --> CDN
    CDN --> ALB
    ALB --> NLB
    
    NLB --> GW1
    NLB --> GW2
    NLB --> GW3
    
    GW1 --> AUTH1
    GW2 --> AUTH2
    GW3 --> AUTH3
    
    AUTH1 --> MYSQL_M
    AUTH2 --> MYSQL_R1
    AUTH3 --> MYSQL_R2
    
    AUTH1 --> REDIS_CLUSTER
    AUTH2 --> REDIS_CLUSTER
    AUTH3 --> REDIS_CLUSTER
    
    MYSQL_M -.-> MYSQL_R1
    MYSQL_M -.-> MYSQL_R2
    
    style AUTH1 fill:#e3f2fd
    style AUTH2 fill:#e3f2fd
    style AUTH3 fill:#e3f2fd
```

## 6. CI/CD 파이프라인

```mermaid
flowchart LR
    subgraph "개발 단계"
        DEV[개발자 코드 작성]
        COMMIT[Git Commit & Push]
    end
    
    subgraph "CI 단계"
        TRIGGER[GitHub Actions 트리거]
        BUILD[Gradle Build]
        TEST[단위 테스트 실행]
        QUALITY[코드 품질 검사]
        SECURITY[보안 스캔]
    end
    
    subgraph "CD 단계"
        DOCKER_BUILD[Docker 이미지 빌드]
        REGISTRY[Container Registry 푸시]
        DEPLOY_STG[Staging 배포]
        E2E_TEST[E2E 테스트]
        DEPLOY_PROD[Production 배포]
    end
    
    subgraph "모니터링"
        HEALTH[헬스체크]
        METRICS[메트릭 수집]
        LOGS[로그 수집]
        ALERTS[알림 설정]
    end
    
    DEV --> COMMIT
    COMMIT --> TRIGGER
    TRIGGER --> BUILD
    BUILD --> TEST
    TEST --> QUALITY
    QUALITY --> SECURITY
    
    SECURITY --> DOCKER_BUILD
    DOCKER_BUILD --> REGISTRY
    REGISTRY --> DEPLOY_STG
    DEPLOY_STG --> E2E_TEST
    E2E_TEST --> DEPLOY_PROD
    
    DEPLOY_PROD --> HEALTH
    HEALTH --> METRICS
    METRICS --> LOGS
    LOGS --> ALERTS
    
    style BUILD fill:#e8f5e8
    style TEST fill:#e8f5e8
    style DEPLOY_PROD fill:#e3f2fd
```

## 7. 보안 아키텍처

```mermaid
graph TB
    subgraph "DMZ (비무장 지대)"
        WAF[Web Application Firewall]
        LB[Load Balancer + SSL]
    end
    
    subgraph "Public Subnet"
        GW[API Gateway]
        BASTION[Bastion Host]
    end
    
    subgraph "Private Subnet"
        AUTH[Auth Service]
        OTHER[Other Services]
    end
    
    subgraph "Data Subnet"
        DB[(Database)]
        CACHE[(Redis Cache)]
    end
    
    subgraph "Security Services"
        KMS[Key Management Service]
        VAULT[Secret Vault]
        IAM[Identity & Access Management]
    end
    
    INTERNET[Internet] --> WAF
    WAF --> LB
    LB --> GW
    
    GW --> AUTH
    GW --> OTHER
    
    AUTH --> DB
    AUTH --> CACHE
    
    AUTH --> KMS
    AUTH --> VAULT
    
    BASTION -.-> AUTH
    BASTION -.-> DB
    
    IAM -.-> GW
    IAM -.-> AUTH
    
    style WAF fill:#ffcdd2
    style KMS fill:#ffcdd2
    style VAULT fill:#ffcdd2
    style IAM fill:#ffcdd2
    style AUTH fill:#e3f2fd
```

## 8. 데이터베이스 샤딩 전략

```mermaid
graph TB
    subgraph "Application Layer"
        APP1[Auth Service Instance 1]
        APP2[Auth Service Instance 2]
        APP3[Auth Service Instance 3]
    end
    
    subgraph "Database Router"
        ROUTER[Sharding Router<br/>Headquarters ID 기반]
    end
    
    subgraph "Shard 1 (HQ 1-100)"
        MASTER1[Master DB 1]
        REPLICA1[Replica DB 1]
    end
    
    subgraph "Shard 2 (HQ 101-200)"
        MASTER2[Master DB 2]
        REPLICA2[Replica DB 2]
    end
    
    subgraph "Shard 3 (HQ 201-300)"
        MASTER3[Master DB 3]
        REPLICA3[Replica DB 3]
    end
    
    subgraph "Metadata"
        CONFIG_DB[(Configuration DB<br/>샤딩 룰 저장)]
    end
    
    APP1 --> ROUTER
    APP2 --> ROUTER
    APP3 --> ROUTER
    
    ROUTER --> CONFIG_DB
    
    ROUTER --> MASTER1
    ROUTER --> MASTER2
    ROUTER --> MASTER3
    
    ROUTER -.-> REPLICA1
    ROUTER -.-> REPLICA2
    ROUTER -.-> REPLICA3
    
    MASTER1 -.-> REPLICA1
    MASTER2 -.-> REPLICA2
    MASTER3 -.-> REPLICA3
    
    style ROUTER fill:#fff3e0
    style CONFIG_DB fill:#f3e5f5
```

이러한 배포 및 시스템 구성 다이어그램들을 통해 Auth Service가 실제 운영 환경에서 어떻게 확장되고 관리되는지를 포트폴리오에서 효과적으로 보여줄 수 있습니다!