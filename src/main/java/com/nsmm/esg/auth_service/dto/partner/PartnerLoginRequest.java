package com.nsmm.esg.auth_service.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 협력사 로그인 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 로그인 요청")
public class PartnerLoginRequest {

  @NotBlank(message = "본사 계정번호는 필수입니다")
  @Schema(description = "본사 계정번호", example = "17250676")
  private String hqAccountNumber;

  @NotBlank(message = "계층적 아이디는 필수입니다")
  @Schema(description = "계층적 아이디", example = "p1-kcs01")
  private String hierarchicalId;

  @NotBlank(message = "비밀번호는 필수입니다")
  @Schema(description = "비밀번호", example = "newPassword123!")
  private String password;
}