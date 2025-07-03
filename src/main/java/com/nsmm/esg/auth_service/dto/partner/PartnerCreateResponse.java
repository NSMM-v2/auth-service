package com.nsmm.esg.auth_service.dto.partner;

import com.nsmm.esg.auth_service.entity.Partner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 협력사 생성 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 생성 응답")
public class PartnerCreateResponse {

  @Schema(description = "협력사 ID", example = "1")
  private Long partnerId;

  @Schema(description = "본사 계정번호", example = "2412161700")
  private String hqAccountNumber;

  @Schema(description = "계층적 아이디", example = "L1-001")
  private String hierarchicalId;

  @Schema(description = "전체 계정번호", example = "2412161700-L1-001")
  private String fullAccountNumber;

  @Schema(description = "회사명", example = "케이씨에스정보통신")
  private String companyName;

  @Schema(description = "초기 비밀번호 (계층적 아이디와 동일)", example = "L1-001")
  private String initialPassword;

  @Schema(description = "협력사 레벨", example = "1")
  private Integer level;

  @Schema(description = "트리 경로", example = "/1/L1-001/")
  private String treePath;

  @Schema(description = "생성 일시")
  private LocalDateTime createdAt;

  @Schema(description = "메시지", example = "협력사가 성공적으로 생성되었습니다.")
  private String message;

  /**
   * Entity를 CreateResponse DTO로 변환
   */
  public static PartnerCreateResponse from(Partner partner) {
    return PartnerCreateResponse.builder()
        .partnerId(partner.getPartnerId())
        .hqAccountNumber(partner.getHqAccountNumber())
        .hierarchicalId(partner.getHierarchicalId())
        .fullAccountNumber(partner.getFullAccountNumber())
        .companyName(partner.getCompanyName())
        .initialPassword(partner.getHierarchicalId()) // 초기 비밀번호는 계층적 아이디와 동일
        .level(partner.getLevel())
        .treePath(partner.getTreePath())
        .createdAt(partner.getCreatedAt())
        .message("협력사가 성공적으로 생성되었습니다. 초기 비밀번호로 로그인 후 비밀번호를 변경해주세요.")
        .build();
  }
}