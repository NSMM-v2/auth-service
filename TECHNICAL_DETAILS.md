# Auth Service 기술 상세 문서

## 1. JWT 구현 세부사항

### JwtUtil 클래스 핵심 기능

```java
@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessTokenExpiration = 900000;  // 15분
    private final long refreshTokenExpiration = 604800000;  // 7일
    
    public String generateAccessToken(JwtClaims claims) {
        Map<String, Object> claimsMap = createClaimsMap(claims);
        return Jwts.builder()
                .setClaims(claimsMap)
                .setSubject(claims.getAccountNumber())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }
}
```

### JWT 클레임 구조

```java
@Builder
public class JwtClaims {
    private String accountNumber;      // 계정번호 (본사/협력사 구분)
    private String companyName;        // 회사명
    private String userType;          // HEADQUARTERS / PARTNER
    private Integer level;            // 협력사 계층 레벨 (본사는 null)
    private String treePath;          // 계층 경로 (본사는 null)
    private Long headquartersId;      // 본사 ID
    private Long partnerId;           // 협력사 ID (본사는 null)
}
```

### HttpOnly 쿠키 설정

```java
private void setJwtCookie(HttpServletResponse response, String token) {
    Cookie jwtCookie = new Cookie("jwt", token);
    jwtCookie.setHttpOnly(true);      // XSS 방지
    jwtCookie.setSecure(cookieSecure); // HTTPS에서만 전송
    jwtCookie.setPath("/");           // 모든 경로에서 사용
    jwtCookie.setMaxAge((int) (jwtUtil.getAccessTokenExpiration() / 1000));
    response.addCookie(jwtCookie);
}
```

## 2. 계층적 권한 시스템

### TreePath 알고리즘

```java
// 협력사 생성 시 TreePath 자동 생성
public String generateTreePath(Partner parentPartner, String hierarchicalId) {
    if (parentPartner == null) {
        // 1차 협력사: /본사ID/L1-001/
        return "/" + headquartersId + "/" + hierarchicalId + "/";
    } else {
        // 하위 협력사: 상위 경로 + 본인 ID
        return parentPartner.getTreePath() + hierarchicalId + "/";
    }
}
```

### 권한 검증 로직

```java
@PreAuthorize("hasRole('HEADQUARTERS') or " +
             "(hasRole('PARTNER') and @securityUtil.canAccessPartner(#partnerId))")
public ResponseEntity<PartnerResponse> getPartner(@PathVariable Long partnerId) {
    // 컨트롤러 로직
}

// SecurityUtil에서 권한 검증
public boolean canAccessPartner(Long targetPartnerId) {
    String currentUserType = getCurrentUserType();
    
    if ("HEADQUARTERS".equals(currentUserType)) {
        return true; // 본사는 모든 접근 가능
    }
    
    if ("PARTNER".equals(currentUserType)) {
        String currentTreePath = getCurrentTreePath();
        String targetTreePath = getPartnerTreePath(targetPartnerId);
        
        // 현재 사용자의 TreePath로 시작하는지 확인 (하위 조직)
        return targetTreePath.startsWith(currentTreePath);
    }
    
    return false;
}
```

## 3. 보안 설정 상세

### Spring Security 설정

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers(
                    "/api/v1/auth/headquarters/register",
                    "/api/v1/auth/headquarters/login",
                    "/api/v1/auth/partners/login"
                ).permitAll()
                // 인증 필요 엔드포인트
                .anyRequest().authenticated())
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil), 
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler));
        
        return http.build();
    }
}
```

### JWT 인증 필터

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        String token = extractTokenFromCookie(request);
        
        if (token != null && jwtUtil.validateToken(token)) {
            JwtClaims claims = jwtUtil.getClaimsFromToken(token);
            
            // Spring Security Context에 인증 정보 설정
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    claims, null, getAuthorities(claims.getUserType()));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

## 4. 데이터베이스 최적화

### 인덱스 전략

```sql
-- 본사 테이블 인덱스
CREATE INDEX idx_headquarters_uuid ON headquarters(headquarters_uuid);
CREATE INDEX idx_email ON headquarters(email);
CREATE INDEX idx_hq_account_number ON headquarters(hq_account_number);

-- 협력사 테이블 인덱스
CREATE INDEX idx_partner_uuid ON partners(partner_uuid);
CREATE INDEX idx_tree_path ON partners(tree_path);
CREATE INDEX idx_headquarters_id ON partners(headquarters_id);
CREATE INDEX idx_hq_account_hierarchical ON partners(hq_account_number, hierarchical_id);

-- 계층 구조 조회 최적화
CREATE INDEX idx_parent_partner_id ON partners(parent_partner_id);
CREATE INDEX idx_level ON partners(level);
```

### JPA 쿼리 최적화

```java
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    
    // 복합 인덱스 활용한 로그인 쿼리
    @Query("SELECT p FROM Partner p " +
           "WHERE p.hqAccountNumber = :hqAccountNumber " +
           "AND p.hierarchicalId = :hierarchicalId")
    Optional<Partner> findByAccountAndHierarchicalId(
        @Param("hqAccountNumber") String hqAccountNumber,
        @Param("hierarchicalId") String hierarchicalId);
    
    // TreePath 기반 하위 조직 조회
    @Query("SELECT p FROM Partner p " +
           "WHERE p.treePath LIKE CONCAT(:parentTreePath, '%') " +
           "AND p.level = :level")
    List<Partner> findChildrenByTreePathAndLevel(
        @Param("parentTreePath") String parentTreePath,
        @Param("level") Integer level);
}
```

## 5. 에러 처리 및 예외 관리

### 글로벌 예외 핸들러

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<String>> handleJwtException(JwtException e) {
        log.error("JWT 처리 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("인증 토큰이 유효하지 않습니다.", "INVALID_TOKEN"));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(AccessDeniedException e) {
        log.error("접근 권한 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("해당 리소스에 접근할 권한이 없습니다.", "ACCESS_DENIED"));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(
            MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(errorMessage, "VALIDATION_FAILED"));
    }
}
```

### 커스텀 예외 클래스

```java
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}

public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) {
        super(message);
    }
}

public class InvalidTreePathException extends RuntimeException {
    public InvalidTreePathException(String message) {
        super(message);
    }
}
```

## 6. 테스트 전략

### 단위 테스트 예시

```java
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Test
    void 액세스토큰_생성_성공() {
        // Given
        JwtClaims claims = JwtClaims.builder()
            .accountNumber("2412161700")
            .userType("HEADQUARTERS")
            .headquartersId(1L)
            .build();
        
        // When
        String token = jwtUtil.generateAccessToken(claims);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }
}
```

### 통합 테스트 예시

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class HeadquartersControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void 본사_로그인_성공() throws Exception {
        // Given
        HeadquartersLoginRequest request = HeadquartersLoginRequest.builder()
            .email("test@company.com")
            .password("password123")
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/headquarters/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(cookie().exists("jwt"));
    }
}
```

## 7. 성능 모니터링

### 메트릭 수집

```java
@Component
public class AuthMetrics {
    private final Counter loginAttempts;
    private final Counter successfulLogins;
    private final Timer jwtGenerationTime;
    
    public AuthMetrics(MeterRegistry meterRegistry) {
        this.loginAttempts = Counter.builder("auth.login.attempts")
            .description("Total login attempts")
            .register(meterRegistry);
            
        this.successfulLogins = Counter.builder("auth.login.successful")
            .description("Successful logins")
            .register(meterRegistry);
            
        this.jwtGenerationTime = Timer.builder("auth.jwt.generation.time")
            .description("JWT token generation time")
            .register(meterRegistry);
    }
}
```

### 로깅 전략

```yaml
logging:
  level:
    com.nsmm.esg.auth_service: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/auth-service.log
    max-history: 30
    max-file-size: 100MB
```

이 문서들을 통해 Auth Service의 기술적 깊이와 구현 품질을 포트폴리오에서 효과적으로 어필할 수 있습니다.