package com.nsmm.esg.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JWT 클레임 정보 DTO
 * 
 * 특징: JWT 토큰에 포함되는 사용자 정보
 * 용도: 토큰 생성/검증, Gateway Service 헤더 변환
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {

  private String accountNumber; // 계정 번호 (HQ001, HQ001-L1-001)
  private String companyName; // 회사명
  private String userType; // "HEADQUARTERS" 또는 "PARTNER"
  private Integer level; // 협력사인 경우 레벨 정보
  private String treePath; // 협력사인 경우 트리 경로 (/1/2/5/)
  private Long headquartersId; // 본사 ID (항상 존재)
  private Long partnerId; // 협력사인 경우에만 존재, 본사인 경우 null
}