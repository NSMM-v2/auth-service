package com.nsmm.esg.auth_service.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 협력사 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 생성 요청")
public class PartnerCreateRequest {

  @NotBlank(message = "회사명은 필수입니다")
  @Size(max = 255, message = "회사명은 255자를 초과할 수 없습니다")
  @Schema(description = "회사명", example = "케이씨에스정보통신")
  private String companyName;

  @NotBlank(message = "이메일은 필수입니다")
  @Email(message = "올바른 이메일 형식이 아닙니다")
  @Schema(description = "이메일", example = "partner@kcs.co.kr")
  private String email;

  @NotBlank(message = "담당자명은 필수입니다")
  @Size(max = 100, message = "담당자명은 100자를 초과할 수 없습니다")
  @Schema(description = "담당자명", example = "김협력")
  private String contactPerson;

  @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
  @Schema(description = "연락처", example = "02-1234-5678")
  private String phone;

  @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 456")
  private String address;
}