package com.nsmm.esg.auth_service.dto.headquarters;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 본사 로그인 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "본사 로그인 요청")
public class HeadquartersLoginRequest {

  @NotBlank(message = "이메일은 필수입니다")
  @Email(message = "올바른 이메일 형식이 아닙니다")
  @Schema(description = "이메일", example = "admin@nsm.co.kr")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다")
  @Schema(description = "비밀번호", example = "Password123!")
  private String password;
}