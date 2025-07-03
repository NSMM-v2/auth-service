package com.nsmm.esg.auth_service.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 협력사 초기 비밀번호 변경 요청 DTO (계정번호 기반)
 * 
 * 프론트엔드 요구사항:
 * - accountNumber: 전체 계정번호
 * - email: 이메일 (선택사항)
 * - temporaryPassword: 현재 임시 비밀번호
 * - newPassword: 새로운 비밀번호
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 초기 비밀번호 변경 요청 (계정번호 기반)")
public class PartnerInitialPasswordChangeByAccountRequest {

  @NotBlank(message = "계정번호는 필수입니다")
  @Schema(description = "전체 계정번호", example = "HQ001-L1-001")
  private String accountNumber;

  @Schema(description = "이메일", example = "partner@company.com")
  private String email;

  @NotBlank(message = "임시 비밀번호는 필수입니다")
  @Schema(description = "현재 임시 비밀번호")
  private String temporaryPassword;

  @NotBlank(message = "새 비밀번호는 필수입니다")
  @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
  @Schema(description = "새 비밀번호")
  private String newPassword;
}