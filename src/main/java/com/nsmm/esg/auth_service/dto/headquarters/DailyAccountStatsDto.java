package com.nsmm.esg.auth_service.dto.headquarters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 날짜별 계정번호 생성 통계 DTO
 * 
 * 본사 계정번호 생성 현황과 통계 정보를 담는 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAccountStatsDto {
  private String date; // 날짜 (YYMMDD)
  private int totalGenerated; // 오늘 생성된 개수
  private int maxDailyCapacity; // 하루 최대 생성 가능 개수 (100개)
  private int remaining; // 오늘 남은 개수
  private String nextAccountNumber; // 다음 계정번호 (예: 2412161700)
  private String sequenceRange; // 순번 범위 (1700-1799)
}