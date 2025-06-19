package com.nsmm.esg.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 토큰 갱신 요청 DTO
 * 
 * 특징: Access Token 만료 시 Refresh Token으로 갱신 요청
 * 용도: JWT 토큰 자동 갱신 API
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

  private String refreshToken; // Refresh Token (7일)
}