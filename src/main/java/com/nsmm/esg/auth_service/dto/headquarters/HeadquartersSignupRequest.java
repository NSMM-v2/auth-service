package com.nsmm.esg.auth_service.dto.headquarters;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 본사 회원가입 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "본사 회원가입 요청")
public class HeadquartersSignupRequest {

  @NotBlank(message = "회사명은 필수입니다")
  @Size(max = 255, message = "회사명은 255자를 초과할 수 없습니다")
  @Schema(description = "회사명", example = "나우앤솔루션즈")
  private String companyName;

  @NotBlank(message = "이메일은 필수입니다")
  @Email(message = "올바른 이메일 형식이 아닙니다")
  @Schema(description = "이메일 (로그인 ID)", example = "admin@nsm.co.kr")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다")
  @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
  @Schema(description = "비밀번호", example = "Password123!")
  private String password;

  @NotBlank(message = "담당자명은 필수입니다")
  @Size(max = 100, message = "담당자명은 100자를 초과할 수 없습니다")
  @Schema(description = "담당자명", example = "김본사")
  private String name;

  @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다")
  @Schema(description = "부서명", example = "ESG관리팀")
  private String department;

  @Size(max = 50, message = "직급은 50자를 초과할 수 없습니다")
  @Schema(description = "직급", example = "팀장")
  private String position;

  @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
  @Schema(description = "연락처", example = "02-1234-5678")
  private String phone;

  @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
  private String address;
}