package com.nsmm.esg.auth_service.dto.partner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 협력사 생성 요청 DTO (DART API 기반)
 * 
 * 입력 필드:
 * - UUID: 다른 서비스에서 DART API를 통해 제공하는 회사 고유 식별자
 * - contactPerson: DART API에서 제공하는 대표자명
 * - companyName: DART API에서 제공하는 회사명
 * - address: DART API에서 제공하는 회사 주소
 * - parentUuid: 상위 협력사 UUID (1차 협력사면 null)
 * 
 * 서버에서 자동 생성:
 * - hierarchicalId: L1-001, L2-001...
 * - treePath: /{본사ID}/L1-001/...
 * - password: 계층적 ID와 동일
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 생성 요청 (DART API 기반)")
public class PartnerCreateRequest {

  @NotBlank(message = "UUID는 필수입니다")
  @Size(max = 36, message = "UUID는 36자를 초과할 수 없습니다")
  @Schema(description = "DART API에서 제공하는 회사 고유 식별자", example = "123e4567-e89b-12d3-a456-426614174000")
  private String uuid; // DART API에서 제공하는 회사 UUID

  @NotBlank(message = "대표자명은 필수입니다")
  @Size(max = 100, message = "대표자명은 100자 이하여야 합니다")
  @Schema(description = "DART API에서 제공하는 대표자명", example = "김협력")
  private String contactPerson; // DART API 대표자명

  @NotBlank(message = "회사명은 필수입니다")
  @Size(max = 255, message = "회사명은 255자 이하여야 합니다")
  @Schema(description = "DART API에서 제공하는 회사명", example = "케이씨에스정보통신")
  private String companyName; // DART API 회사명

  @Size(max = 500, message = "주소는 500자 이하여야 합니다")
  @Schema(description = "DART API에서 제공하는 회사 주소", example = "서울특별시 강남구 테헤란로 456")
  private String address; // DART API 회사 주소

  @Size(max = 36, message = "상위 협력사 UUID는 36자를 초과할 수 없습니다")
  @Schema(description = "상위 협력사 UUID (1차 협력사면 null)", example = "123e4567-e89b-12d3-a456-426614174000")
  private String parentUuid; // 상위 협력사 UUID (1차 협력사면 null)

  @Size(max = 20, message = "연락처는 20자 이하여야 합니다")
  @Schema(description = "DART API에서 제공하는 연락처", example = "02-1234-5678")
  private String phone; // DART API 연락처 (선택적)
}