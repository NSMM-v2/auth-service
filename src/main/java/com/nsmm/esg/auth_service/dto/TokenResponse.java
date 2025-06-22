package com.nsmm.esg.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JWT 토큰 응답 DTO
 * 
 * 특징: 로그인 성공 시 JWT 토큰과 사용자 정보 반환
 * 용도: 본사/협력사 로그인 응답, 토큰 갱신 응답
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

  private String accessToken; // Access Token (15분)
  private String refreshToken; // Refresh Token (7일)
  private String tokenType; // Bearer
  private Long expiresIn; // 만료 시간 (밀리초)
  private LocalDateTime issuedAt; // 발급 시간
  private LocalDateTime expiresAt; // 만료 시간

  // 사용자 정보
  private String accountNumber; // 계정 번호 (HQ001, HQ001-L1-001)
  private String companyName; // 회사명
  private String userType; // "HEADQUARTERS" 또는 "PARTNER"
  private Integer level; // 협력사인 경우 레벨 정보
  private Boolean passwordChanged; // 비밀번호 변경 여부 (협력사만)

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

  /**
   * 협력사용 토큰 응답 생성 (passwordChanged 포함)
   */
  public static TokenResponse ofPartner(String accessToken, String refreshToken,
      Long expiresIn, String accountNumber,
      String companyName, String userType, Integer level, Boolean passwordChanged) {
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
        .passwordChanged(passwordChanged)
        .build();
  }
}