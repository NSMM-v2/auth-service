package com.nsmm.esg.auth_service.config;

import com.nsmm.esg.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * JWT 기반 인증/인가 시스템
 * 
 * Gateway 라우팅에 맞춘 /auth prefix 적용:
 * - HeadquartersController: /api/v1/auth/headquarters/**
 * - PartnerController: /api/v1/auth/partners/**
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화
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

                                // 세션 비활성화 (JWT 사용)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 요청 권한 설정
                                .authorizeHttpRequests(auth -> auth
                                                // === 공개 엔드포인트 (인증 불필요) ===
                                                .requestMatchers(
                                                                // 본사 회원가입/로그인/로그아웃
                                                                "/api/v1/auth/headquarters/register",
                                                                "/api/v1/auth/headquarters/login",
                                                                "/api/v1/auth/headquarters/logout",
                                                                "/api/v1/auth/headquarters/check-email",
                                                                "/api/v1/auth/headquarters/check-uuid",
                                                                "/api/v1/auth/headquarters/by-uuid/*",
                                                                "/api/v1/auth/headquarters/next-account-number",
                                                                "/api/v1/auth/headquarters/validate-account-number",

                                                                // 협력사 로그인/로그아웃 및 공개 API
                                                                "/api/v1/auth/partners/login",
                                                                "/api/v1/auth/partners/logout",
                                                                "/api/v1/auth/partners/check-email",
                                                                "/api/v1/auth/partners/check-uuid",

                                                                // 시스템 관련
                                                                "/actuator/**",
                                                                "/error",

                                                                // Swagger UI 관련 경로
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/api-docs/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // === 본사 전용 엔드포인트 ===
                                                .requestMatchers(
                                                                // 1차 협력사 생성 및 관리 (본사만 가능)
                                                                "/api/v1/auth/partners/first-level",
                                                                "/api/v1/auth/partners/unchanged-password")
                                                .hasRole("HEADQUARTERS")

                                                // === 협력사 전용 엔드포인트 ===
                                                .requestMatchers(
                                                                // 하위 협력사 생성 (협력사만 가능)
                                                                "/api/v1/auth/partners/{parentId}/sub-partners")
                                                .hasRole("PARTNER")

                                                // === 인증된 사용자 공통 엔드포인트 (@PreAuthorize로 세부 권한 제어) ===
                                                .requestMatchers(
                                                                // 현재 사용자 정보 조회
                                                                "/api/v1/auth/headquarters/me",
                                                                "/api/v1/auth/partners/me",

                                                                // UUID 기반 협력사 생성
                                                                "/api/v1/auth/partners/create-by-uuid",

                                                                // 협력사 정보 조회
                                                                "/api/v1/auth/partners/{partnerId}",
                                                                "/api/v1/auth/partners/by-uuid/{uuid}",

                                                                // 접근 가능한 협력사 목록
                                                                "/api/v1/auth/partners/accessible",

                                                                // 하위 협력사 목록 조회
                                                                "/api/v1/auth/partners/{parentId}/children",

                                                                // 초기 비밀번호 변경
                                                                "/api/v1/auth/partners/{partnerId}/initial-password")
                                                .hasAnyRole("HEADQUARTERS", "PARTNER")

                                                // 나머지 모든 요청은 인증 필요
                                                .anyRequest().authenticated())

                                // 예외 처리 설정
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler))

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

}