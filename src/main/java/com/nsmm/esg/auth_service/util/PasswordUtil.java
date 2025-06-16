package com.nsmm.esg.auth_service.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 비밀번호 생성 및 암호화 유틸리티 (AWS IAM 방식)
 */
@Slf4j
@Component
public class PasswordUtil {

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    // 임시 비밀번호 생성 규칙
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*";

    public PasswordUtil() {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    /**
     * 임시 비밀번호 생성 (AWS IAM 방식)
     * - 최소 12자
     * - 대문자, 소문자, 숫자, 특수문자 각각 최소 1개 포함
     */
    public String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();

        // 각 문자 유형별로 최소 1개씩 추가
        password.append(getRandomChar(UPPERCASE_CHARS));
        password.append(getRandomChar(LOWERCASE_CHARS));
        password.append(getRandomChar(DIGIT_CHARS));
        password.append(getRandomChar(SPECIAL_CHARS));

        // 나머지 길이만큼 랜덤 문자 추가
        String allChars = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
        for (int i = 4; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(getRandomChar(allChars));
        }

        // 문자 순서 섞기
        return shuffleString(password.toString());
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

    /**
     * 비밀번호 강도 검증
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> SPECIAL_CHARS.indexOf(ch) >= 0);

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * 계정 번호 기반 초기 비밀번호 생성 (선택사항)
     */
    public String generateAccountBasedPassword(String accountNumber) {
        // 계정 번호의 일부와 랜덤 문자열 조합
        String suffix = RandomStringUtils.randomAlphanumeric(6);
        String basePassword = accountNumber.substring(0, Math.min(6, accountNumber.length())) + suffix;
        
        // 비밀번호 규칙에 맞게 조정
        return ensurePasswordComplexity(basePassword);
    }

    /**
     * 문자열에서 랜덤 문자 선택
     */
    private char getRandomChar(String chars) {
        return chars.charAt(secureRandom.nextInt(chars.length()));
    }

    /**
     * 문자열 순서 섞기
     */
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    /**
     * 비밀번호 복잡성 보장
     */
    private String ensurePasswordComplexity(String password) {
        StringBuilder result = new StringBuilder(password);
        
        // 대문자가 없으면 추가
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            result.append(getRandomChar(UPPERCASE_CHARS));
        }
        
        // 소문자가 없으면 추가
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            result.append(getRandomChar(LOWERCASE_CHARS));
        }
        
        // 숫자가 없으면 추가
        if (!password.chars().anyMatch(Character::isDigit)) {
            result.append(getRandomChar(DIGIT_CHARS));
        }
        
        // 특수문자가 없으면 추가
        if (!password.chars().anyMatch(ch -> SPECIAL_CHARS.indexOf(ch) >= 0)) {
            result.append(getRandomChar(SPECIAL_CHARS));
        }
        
        return shuffleString(result.toString());
    }

    /**
     * 비밀번호 만료 확인 (선택사항 - 추후 구현)
     */
    public boolean isPasswordExpired(java.time.LocalDateTime lastPasswordChange, int expirationDays) {
        if (lastPasswordChange == null) {
            return true;
        }
        return lastPasswordChange.plusDays(expirationDays).isBefore(java.time.LocalDateTime.now());
    }

    /**
     * 비밀번호 히스토리 검증 (선택사항 - 추후 구현)
     */
    public boolean isPasswordInHistory(String newPassword, java.util.List<String> passwordHistory) {
        return passwordHistory.stream()
                .anyMatch(oldPassword -> matches(newPassword, oldPassword));
    }
} 