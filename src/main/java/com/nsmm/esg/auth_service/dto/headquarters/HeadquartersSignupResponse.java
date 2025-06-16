package com.nsmm.esg.auth_service.dto.headquarters;

import com.nsmm.esg.auth_service.entity.Headquarters;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 본사 회원가입 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "본사 회원가입 응답")
public class HeadquartersSignupResponse {

  @Schema(description = "본사 ID")
  private Long id;

  @Schema(description = "본사 계정번호", example = "2412161700")
  private String hqAccountNumber;

  @Schema(description = "회사명")
  private String companyName;

  @Schema(description = "이메일")
  private String email;

  @Schema(description = "담당자명")
  private String name;

  @Schema(description = "부서명")
  private String department;

  @Schema(description = "직급")
  private String position;

  @Schema(description = "연락처")
  private String phone;

  @Schema(description = "주소")
  private String address;

  @Schema(description = "상태")
  private Headquarters.CompanyStatus status;

  @Schema(description = "생성 일시")
  private LocalDateTime createdAt;

  @Schema(description = "메시지")
  private String message;

  /**
   * Entity를 SignupResponse DTO로 변환
   */
  public static HeadquartersSignupResponse from(Headquarters headquarters) {
    return HeadquartersSignupResponse.builder()
        .id(headquarters.getId())
        .hqAccountNumber(headquarters.getHqAccountNumber())
        .companyName(headquarters.getCompanyName())
        .email(headquarters.getEmail())
        .name(headquarters.getName())
        .department(headquarters.getDepartment())
        .position(headquarters.getPosition())
        .phone(headquarters.getPhone())
        .address(headquarters.getAddress())
        .status(headquarters.getStatus())
        .createdAt(headquarters.getCreatedAt())
        .message("본사 계정이 성공적으로 생성되었습니다.")
        .build();
  }
}