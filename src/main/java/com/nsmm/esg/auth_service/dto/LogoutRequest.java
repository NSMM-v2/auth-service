package com.nsmm.esg.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그아웃 요청 DTO
 * 
 * 특징: 로그아웃 시 토큰 무효화 요청
 * 용도: 안전한 로그아웃 처리, 토큰 블랙리스트 관리
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

  private String accessToken; // Access Token
  private String refreshToken; // Refresh Token
}