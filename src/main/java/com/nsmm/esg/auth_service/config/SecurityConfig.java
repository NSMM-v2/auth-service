package com.nsmm.esg.auth_service.config;

import com.nsmm.esg.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정
 * JWT 기반 인증/인가 시스템
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * 비밀번호 암호화 Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security Filter Chain 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용으로 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 비활성화 (JWT 사용)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트 (인증 불필요)
                .requestMatchers(
                    "/api/v1/headquarters/signup",
                    "/api/v1/headquarters/login",
                    "/api/v1/headquarters/check-email",
                    "/api/v1/partners/login",
                    "/actuator/**"
                ).permitAll()
                
                // 본사 전용 엔드포인트
                .requestMatchers(
                    "/api/v1/headquarters/**",
                    "/api/v1/partners/create",
                    "/api/v1/partners/*/status",
                    "/api/v1/partners/password-change-required"
                ).hasRole("HEADQUARTERS")
                
                // 협력사 전용 엔드포인트
                .requestMatchers(
                    "/api/v1/partners/*/password",
                    "/api/v1/partners/*/subtree"
                ).hasRole("PARTNER")
                
                // 본사 또는 협력사 모두 접근 가능
                .requestMatchers(
                    "/api/v1/partners/**"
                ).hasAnyRole("HEADQUARTERS", "PARTNER")
                
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // H2 Console 사용을 위한 설정
            .headers(headers -> headers.frameOptions().disable())
            
            // 예외 처리 설정
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            
            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT 인증 필터 Bean
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
} 