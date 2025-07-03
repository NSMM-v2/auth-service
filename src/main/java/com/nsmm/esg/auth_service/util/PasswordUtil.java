package com.nsmm.esg.auth_service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 비밀번호 관련 유틸리티 클래스 (보안 강화 + 사용자 친화적)
 *
 * 기능:
 * - AWS IAM 스타일 복잡한 임시 비밀번호 생성
 * - 기억하기 쉬운 단어 조합 비밀번호 생성
 * - 회사명 기반 초기 비밀번호 생성
 * - 비밀번호 강도 검증
 */
@Slf4j
@Component
public class PasswordUtil {

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public PasswordUtil() {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    /**
     * 비밀번호 암호화
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 비밀번호 검증
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

}