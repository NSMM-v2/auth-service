package com.nsmm.esg.auth_service.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 협력사 로그인 요청 DTO
 * 
 * 프론트엔드 요구사항:
 * - accountNumber: 전체 계정번호 (HQ001-L1-001)
 * - email: 이메일 주소
 * - password: 비밀번호
 * 
 * 백엔드 처리:
 * - accountNumber를 파싱하여 hqAccountNumber와 hierarchicalId로 분리
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 로그인 요청")
public class PartnerLoginRequest {

  @NotBlank(message = "본사 계정번호는 필수입니다")
  @Schema(description = "본사 계정번호", example = "HQ001")
  private String hqAccountNumber;

  @NotBlank(message = "협력사 아이디는 필수입니다")
  @Schema(description = "협력사 아이디 (계층형 아이디)", example = "L1-001")
  private String partnerCode;

  @NotBlank(message = "비밀번호는 필수입니다")
  @Schema(description = "비밀번호", example = "newPassword123!")
  private String password;
}