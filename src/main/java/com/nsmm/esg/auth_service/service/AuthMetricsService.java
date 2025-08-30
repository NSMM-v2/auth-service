package com.nsmm.esg.auth_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 인증 서비스 메트릭 수집 서비스
 * 
 * 주요 기능:
 * - 로그인/회원가입/로그아웃 횟수 카운팅
 * - JWT 토큰 발급/검증 메트릭 수집
 * - 보안 이벤트 추적
 * - 비즈니스 로직 메트릭 수집
 * - 활성 사용자 세션 추적
 */
@Service
@Slf4j
public class AuthMetricsService {

    private final MeterRegistry meterRegistry;
    
    // 게이지 메트릭들 (활성 세션 수)
    private final AtomicInteger activeHeadquartersUsers = new AtomicInteger(0);
    private final AtomicInteger activePartnerUsers = new AtomicInteger(0);

    public AuthMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 활성 사용자 세션 게이지
        Gauge.builder("auth_active_headquarters_sessions", activeHeadquartersUsers, AtomicInteger::doubleValue)
                .description("현재 활성 본사 사용자 세션 수")
                .register(meterRegistry);

        Gauge.builder("auth_active_partner_sessions", activePartnerUsers, AtomicInteger::doubleValue)
                .description("현재 활성 협력사 사용자 세션 수")
                .register(meterRegistry);

        log.info("AuthMetricsService 초기화 완료 - 메트릭 수집 시작");
    }

    // ===== 로그인 관련 메트릭 =====

    /**
     * 로그인 시도 카운터 증가
     */
    public void incrementLoginAttempts(String userType, String result) {
        Counter.builder("auth_login_attempts_total")
                .description("총 로그인 시도 횟수")
                .tag("user_type", userType.toLowerCase())
                .tag("result", result.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("로그인 시도 메트릭 기록: userType={}, result={}", userType, result);
    }

    /**
     * 로그인 처리 시간 기록
     */
    public Timer.Sample startLoginTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordLoginDuration(Timer.Sample sample, String userType, String result) {
        sample.stop(Timer.builder("auth_login_duration_seconds")
                .description("로그인 처리 시간")
                .tag("user_type", userType.toLowerCase())
                .tag("result", result.toLowerCase())
                .register(meterRegistry));
        log.debug("로그인 처리시간 메트릭 기록: userType={}, result={}", userType, result);
    }

    // ===== 회원가입 관련 메트릭 =====

    /**
     * 사용자 등록 카운터 증가
     */
    public void incrementUserRegistrations(String userType) {
        Counter.builder("auth_user_registrations_total")
                .description("총 사용자 등록 횟수")
                .tag("user_type", userType.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("사용자 등록 메트릭 기록: userType={}", userType);
    }

    /**
     * 회원가입 처리 시간 기록
     */
    public Timer.Sample startRegistrationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRegistrationDuration(Timer.Sample sample, String userType, String result) {
        sample.stop(Timer.builder("auth_registration_duration_seconds")
                .description("회원가입 처리 시간")
                .tag("user_type", userType.toLowerCase())
                .tag("result", result.toLowerCase())
                .register(meterRegistry));
        log.debug("회원가입 처리시간 메트릭 기록: userType={}, result={}", userType, result);
    }

    // ===== 로그아웃 관련 메트릭 =====

    /**
     * 로그아웃 카운터 증가
     */
    public void incrementLogoutAttempts(String userType) {
        Counter.builder("auth_logout_attempts_total")
                .description("총 로그아웃 횟수")
                .tag("user_type", userType.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("로그아웃 메트릭 기록: userType={}", userType);
    }

    // ===== JWT 관련 메트릭 =====

    /**
     * JWT 토큰 운영 카운터 증가
     */
    public void incrementJwtOperations(String operation, String tokenType) {
        Counter.builder("auth_jwt_operations_total")
                .description("JWT 토큰 운영 횟수")
                .tag("operation", operation.toLowerCase())
                .tag("token_type", tokenType.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("JWT 토큰 운영 메트릭 기록: operation={}, tokenType={}", operation, tokenType);
    }

    // ===== 보안 관련 메트릭 =====

    /**
     * 인증 실패 카운터 증가
     */
    public void incrementAuthFailures(String reason) {
        Counter.builder("auth_failures_total")
                .description("인증 실패 횟수")
                .tag("reason", reason.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("인증 실패 메트릭 기록: reason={}", reason);
    }

    /**
     * 비밀번호 이벤트 카운터 증가
     */
    public void incrementPasswordEvents(String event) {
        Counter.builder("auth_password_events_total")
                .description("비밀번호 관련 이벤트 횟수")
                .tag("event", event.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("비밀번호 이벤트 메트릭 기록: event={}", event);
    }

    // ===== 협력사 관련 메트릭 =====

    /**
     * 협력사 생성 카운터 증가
     */
    public void incrementPartnerCreations(String level, String creatorType) {
        Counter.builder("auth_partner_creations_total")
                .description("협력사 생성 횟수")
                .tag("level", level)
                .tag("creator_type", creatorType.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("협력사 생성 메트릭 기록: level={}, creatorType={}", level, creatorType);
    }

    /**
     * 협력사 생성 처리 시간 기록
     */
    public Timer.Sample startPartnerCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPartnerCreationDuration(Timer.Sample sample, String level, String result) {
        sample.stop(Timer.builder("auth_partner_creation_duration_seconds")
                .description("협력사 생성 처리 시간")
                .tag("level", level)
                .tag("result", result.toLowerCase())
                .register(meterRegistry));
        log.debug("협력사 생성 처리시간 메트릭 기록: level={}, result={}", level, result);
    }

    // ===== 조직 관리 관련 메트릭 =====

    /**
     * 조직 조회 카운터 증가
     */
    public void incrementOrganizationQueries(String queryType) {
        Counter.builder("auth_organization_queries_total")
                .description("조직 정보 조회 횟수")
                .tag("query_type", queryType.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("조직 조회 메트릭 기록: queryType={}", queryType);
    }

    // ===== UUID 검증 관련 메트릭 =====

    /**
     * UUID 검증 카운터 증가
     */
    public void incrementUuidValidations(String entityType, String result) {
        Counter.builder("auth_uuid_validations_total")
                .description("UUID 검증 요청 횟수")
                .tag("entity_type", entityType.toLowerCase())
                .tag("result", result.toLowerCase())
                .register(meterRegistry)
                .increment();
        log.debug("UUID 검증 메트릭 기록: entityType={}, result={}", entityType, result);
    }

    // ===== 활성 세션 관리 =====

    /**
     * 활성 본사 사용자 세션 수 증가
     */
    public void incrementActiveHeadquartersUsers() {
        activeHeadquartersUsers.incrementAndGet();
        log.debug("활성 본사 사용자 세션 증가: {}", activeHeadquartersUsers.get());
    }

    /**
     * 활성 본사 사용자 세션 수 감소
     */
    public void decrementActiveHeadquartersUsers() {
        activeHeadquartersUsers.decrementAndGet();
        log.debug("활성 본사 사용자 세션 감소: {}", activeHeadquartersUsers.get());
    }

    /**
     * 활성 협력사 사용자 세션 수 증가
     */
    public void incrementActivePartnerUsers() {
        activePartnerUsers.incrementAndGet();
        log.debug("활성 협력사 사용자 세션 증가: {}", activePartnerUsers.get());
    }

    /**
     * 활성 협력사 사용자 세션 수 감소
     */
    public void decrementActivePartnerUsers() {
        activePartnerUsers.decrementAndGet();
        log.debug("활성 협력사 사용자 세션 감소: {}", activePartnerUsers.get());
    }

    // ===== 현재 상태 조회 메서드들 =====

    /**
     * 현재 활성 본사 사용자 세션 수 조회
     */
    public int getActiveHeadquartersUsersCount() {
        return activeHeadquartersUsers.get();
    }

    /**
     * 현재 활성 협력사 사용자 세션 수 조회
     */
    public int getActivePartnerUsersCount() {
        return activePartnerUsers.get();
    }
}