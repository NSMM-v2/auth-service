package com.nsmm.esg.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 인증 관련 공통 DTO 클래스들
 */
public class AuthDto {

    /**
     * JWT 토큰 응답 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private LocalDateTime issuedAt;
        private LocalDateTime expiresAt;
        
        // 사용자 정보
        private String accountNumber;
        private String companyName;
        private String userType;  // "HEADQUARTERS" 또는 "PARTNER"
        private Integer level;    // 협력사인 경우 레벨 정보
        
        /**
         * 기본 토큰 응답 생성
         */
        public static TokenResponse of(String accessToken, String refreshToken, 
                                     Long expiresIn, String accountNumber, 
                                     String companyName, String userType, Integer level) {
            LocalDateTime now = LocalDateTime.now();
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(expiresIn / 1000))
                    .accountNumber(accountNumber)
                    .companyName(companyName)
                    .userType(userType)
                    .level(level)
                    .build();
        }
    }

    /**
     * 토큰 갱신 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {
        
        private String refreshToken;
    }

    /**
     * API 응답 공통 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        
        private boolean success;
        private String message;
        private T data;
        private String errorCode;
        private LocalDateTime timestamp;
        
        /**
         * 성공 응답 생성
         */
        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("요청이 성공적으로 처리되었습니다.")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        
        /**
         * 성공 응답 생성 (메시지 포함)
         */
        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        
        /**
         * 실패 응답 생성
         */
        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        
        /**
         * 실패 응답 생성 (에러 코드 포함)
         */
        public static <T> ApiResponse<T> error(String message, String errorCode) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .errorCode(errorCode)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * JWT 클레임 정보 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JwtClaims {
        
        private String accountNumber;
        private String companyName;
        private String userType;  // "HEADQUARTERS" 또는 "PARTNER"
        private Integer level;    // 협력사인 경우 레벨 정보
        private String treePath;  // 협력사인 경우 트리 경로
        private Long headquartersId;  // 본사 ID
        private Long userId;      // 사용자 ID (본사 또는 협력사 ID)
    }

    /**
     * 로그아웃 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogoutRequest {
        
        private String accessToken;
        private String refreshToken;
    }
} 