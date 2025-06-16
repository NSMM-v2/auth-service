package com.nsmm.esg.auth_service.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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

    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*";

    // 기억하기 쉬운 단어들 (영문)
    private static final String[] EASY_WORDS = {
            "Apple", "Banana", "Cherry", "Dragon", "Eagle", "Flower", "Galaxy", "Happy",
            "Island", "Journey", "Kindness", "Light", "Mountain", "Nature", "Ocean", "Peace",
            "Queen", "River", "Sunshine", "Travel", "Unity", "Victory", "Wonder", "Xmas",
            "Yellow", "Zebra", "Bridge", "Castle", "Diamond", "Energy", "Forest", "Garden"
    };

    // 기억하기 쉬운 단어들 (한글 영문 표기)
    private static final String[] KOREAN_WORDS = {
            "Seoul", "Busan", "Incheon", "Daegu", "Gwangju", "Daejeon", "Ulsan", "Sejong",
            "Gangnam", "Myeongdong", "Hongdae", "Itaewon", "Jamsil", "Sinchon", "Gangbuk", "Seocho"
    };

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public PasswordUtil() {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    /**
     * AWS IAM 스타일 임시 비밀번호 생성 (12자 복잡성)
     * 
     * 특징: 높은 보안성, 임시 사용 목적
     * 형태: A3x@9Kw#2Mn$
     */
    public String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();

        // 각 문자 타입별로 최소 1개씩 보장
        password.append(getRandomChar(UPPERCASE_CHARS));
        password.append(getRandomChar(LOWERCASE_CHARS));
        password.append(getRandomChar(DIGIT_CHARS));
        password.append(getRandomChar(SPECIAL_CHARS));

        // 나머지 8자리는 랜덤하게
        String allChars = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
        for (int i = 4; i < 12; i++) {
            password.append(getRandomChar(allChars));
        }

        // 문자 순서 섞기
        return shuffleString(password.toString());
    }

    /**
     * 기억하기 쉬운 단어 조합 비밀번호 생성
     * 
     * 특징: 사용자 친화적, 기억하기 쉬움
     * 형태: Apple123!, Cherry456@, Seoul789#
     * 
     * @return 기억하기 쉬운 비밀번호
     */
    public String generateFriendlyPassword() {
        // 단어 선택 (영문 또는 한글 표기 중 랜덤)
        String[] wordPool = secureRandom.nextBoolean() ? EASY_WORDS : KOREAN_WORDS;
        String word = wordPool[secureRandom.nextInt(wordPool.length)];

        // 3자리 숫자 생성
        int number = secureRandom.nextInt(900) + 100; // 100-999

        // 특수문자 1개 선택
        char specialChar = getRandomChar(SPECIAL_CHARS);

        return word + number + specialChar;
    }

    /**
     * 회사명 기반 초기 비밀번호 생성
     * 
     * 특징: 회사 관련성, 적당한 복잡성
     * 형태: Samsung2024!, LG2024@
     * 
     * @param companyName 회사명
     * @return 회사명 기반 비밀번호
     */
    public String generateCompanyBasedPassword(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            return generateFriendlyPassword();
        }

        // 회사명에서 영문 부분 추출 (최대 8자)
        String cleanCompanyName = companyName.replaceAll("[^a-zA-Z가-힣]", "");
        String companyPrefix = convertCompanyNameToEnglish(cleanCompanyName);

        if (companyPrefix.length() > 8) {
            companyPrefix = companyPrefix.substring(0, 8);
        }

        // 첫 글자만 대문자로
        companyPrefix = companyPrefix.substring(0, 1).toUpperCase() +
                companyPrefix.substring(1).toLowerCase();

        // 현재 연도 추가
        int currentYear = java.time.LocalDate.now().getYear();

        // 특수문자 1개 추가
        char specialChar = getRandomChar(SPECIAL_CHARS);

        return companyPrefix + currentYear + specialChar;
    }

    /**
     * 회사명을 영문으로 변환
     */
    private String convertCompanyNameToEnglish(String companyName) {
        return companyName
                .replace("삼성", "Samsung")
                .replace("엘지", "LG")
                .replace("LG", "LG")
                .replace("현대", "Hyundai")
                .replace("기아", "KIA")
                .replace("에스케이", "SK")
                .replace("SK", "SK")
                .replace("포스코", "POSCO")
                .replace("네이버", "Naver")
                .replace("카카오", "Kakao")
                .replace("롯데", "Lotte")
                .replace("한화", "Hanwha")
                .replace("두산", "Doosan")
                .replace("CJ", "CJ");
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
     * 담당자 이름 기반 초기 비밀번호 생성
     * 
     * 특징: 담당자 관련성, 기억하기 쉬움
     * 형태: Kim2024!, Lee2024@
     * 
     * @param contactPersonName 담당자명
     * @return 담당자 기반 비밀번호
     */
    public String generateContactBasedPassword(String contactPersonName) {
        if (contactPersonName == null || contactPersonName.trim().isEmpty()) {
            return generateFriendlyPassword();
        }

        // 담당자 이름에서 영문 부분 추출 (최대 6자)
        String cleanName = contactPersonName.replaceAll("[^a-zA-Z가-힣]", "");
        String namePrefix = convertNameToEnglish(cleanName);

        if (namePrefix.length() > 6) {
            namePrefix = namePrefix.substring(0, 6);
        }

        // 첫 글자만 대문자로
        namePrefix = namePrefix.substring(0, 1).toUpperCase() +
                namePrefix.substring(1).toLowerCase();

        // 현재 연도 추가
        int currentYear = java.time.LocalDate.now().getYear();

        // 특수문자 1개 추가
        char specialChar = getRandomChar(SPECIAL_CHARS);

        return namePrefix + currentYear + specialChar;
    }

    /**
     * 이름을 영문으로 변환
     */
    private String convertNameToEnglish(String name) {
        // 한글 이름의 경우 성씨를 영문으로 변환
        if (name.matches(".*[가-힣].*")) {
            return name
                    .replace("김", "Kim")
                    .replace("이", "Lee")
                    .replace("박", "Park")
                    .replace("최", "Choi")
                    .replace("정", "Jung")
                    .replace("강", "Kang")
                    .replace("조", "Cho")
                    .replace("윤", "Yoon")
                    .replace("장", "Jang")
                    .replace("임", "Lim")
                    .replace("한", "Han")
                    .replace("오", "Oh")
                    .replace("서", "Seo")
                    .replace("신", "Shin")
                    .replace("권", "Kwon")
                    .replace("황", "Hwang")
                    .replace("안", "Ahn")
                    .replace("송", "Song")
                    .replace("류", "Ryu")
                    .replace("전", "Jeon")
                    .replace("홍", "Hong")
                    .replace("고", "Ko")
                    .replace("문", "Moon")
                    .replace("양", "Yang")
                    .replace("손", "Son")
                    .replace("배", "Bae")
                    .replace("백", "Baek")
                    .replace("허", "Heo")
                    .replace("유", "Yoo")
                    .replace("남", "Nam")
                    .replace("심", "Sim")
                    .replace("노", "Noh")
                    .replace("하", "Ha");
        }

        // 영문 이름인 경우 그대로 반환
        return name;
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