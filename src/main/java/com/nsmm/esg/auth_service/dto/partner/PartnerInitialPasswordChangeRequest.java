package com.nsmm.esg.auth_service.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 협력사 초기 비밀번호 변경 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 초기 비밀번호 변경 요청")
public class PartnerInitialPasswordChangeRequest {

  @NotBlank(message = "새 비밀번호는 필수입니다")
  @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
  @Schema(description = "새 비밀번호")
  private String newPassword;

  @NotBlank(message = "비밀번호 확인은 필수입니다")
  @Schema(description = "비밀번호 확인")
  private String confirmPassword;
}