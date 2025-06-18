package com.nsmm.esg.auth_service.dto.headquarters;

import com.nsmm.esg.auth_service.entity.Headquarters;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 본사 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "본사 정보 응답")
public class HeadquartersResponse {

  @Schema(description = "본사 ID")
  private Long headquartersId;

  @Schema(description = "본사 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
  private String uuid;

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

  @Schema(description = "수정 일시")
  private LocalDateTime updatedAt;

  /**
   * Entity를 DTO로 변환
   */
  public static HeadquartersResponse from(Headquarters headquarters) {
    return HeadquartersResponse.builder()
        .headquartersId(headquarters.getHeadquartersId())
        .uuid(headquarters.getUuid())
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
        .updatedAt(headquarters.getUpdatedAt())
        .build();
  }
}