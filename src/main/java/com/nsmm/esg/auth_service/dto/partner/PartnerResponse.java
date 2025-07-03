package com.nsmm.esg.auth_service.dto.partner;

import com.nsmm.esg.auth_service.entity.Partner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 협력사 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 정보 응답")
public class PartnerResponse {

  @Schema(description = "협력사 ID")
  private Long partnerId;

  @Schema(description = "프론트엔드 UUID")
  private String uuid;

  @Schema(description = "본사 계정번호")
  private String hqAccountNumber;

  @Schema(description = "계층적 아이디")
  private String hierarchicalId;

  @Schema(description = "전체 계정번호")
  private String fullAccountNumber;

  @Schema(description = "계정번호 (프론트엔드 호환용)")
  private String accountNumber;

  @Schema(description = "회사명")
  private String companyName;

  @Schema(description = "사용자 타입", example = "PARTNER")
  private String userType;

  @Schema(description = "협력사 레벨")
  private Integer level;

  @Schema(description = "트리 경로")
  private String treePath;

  @Schema(description = "상태")
  private String status;

  @Schema(description = "비밀번호 변경 여부")
  private Boolean passwordChanged;

  @Schema(description = "생성 일시")
  private LocalDateTime createdAt;

  @Schema(description = "수정 일시")
  private LocalDateTime updatedAt;

  // 관계 정보
  @Schema(description = "상위 협력사 ID")
  private Long parentPartnerId;

  @Schema(description = "상위 협력사명")
  private String parentPartnerName;

  @Schema(description = "본사 ID")
  private Long headquartersId;

  @Schema(description = "본사명")
  private String headquartersName;

  @Schema(description = "직속 하위 레벨 (권한 제어용)")
  private Integer directChildLevel;

  /**
   * Entity를 Response DTO로 변환
   */
  public static PartnerResponse from(Partner partner) {
    return PartnerResponse.builder()
        .partnerId(partner.getPartnerId())
        .uuid(partner.getUuid())
        .hqAccountNumber(partner.getHqAccountNumber())
        .hierarchicalId(partner.getHierarchicalId())
        .fullAccountNumber(partner.getFullAccountNumber())
        .accountNumber(partner.getFullAccountNumber()) // 프론트엔드 호환용
        .companyName(partner.getCompanyName())
        .userType("PARTNER") // 협력사 타입 고정
        .level(partner.getLevel())
        .treePath(partner.getTreePath())
        .status(partner.getStatus().name())
        .passwordChanged(partner.getPasswordChanged())
        .headquartersId(partner.getHeadquarters().getHeadquartersId())
        .headquartersName(partner.getHeadquarters().getCompanyName())
        .parentPartnerId(partner.getParentPartner() != null ? partner.getParentPartner().getPartnerId() : null)
        .parentPartnerName(partner.getParentPartner() != null ? partner.getParentPartner().getCompanyName() : null)
        .directChildLevel(partner.getDirectChildLevel())
        .createdAt(partner.getCreatedAt())
        .updatedAt(partner.getUpdatedAt())
        .build();
  }

  /**
   * 간소화된 협력사 정보 (목록 조회용)
   */
  public static PartnerResponse fromSimple(Partner partner) {
    return PartnerResponse.builder()
        .partnerId(partner.getPartnerId())
        .uuid(partner.getUuid())
        .hierarchicalId(partner.getHierarchicalId())
        .fullAccountNumber(partner.getFullAccountNumber())
        .accountNumber(partner.getFullAccountNumber()) // 프론트엔드 호환용
        .companyName(partner.getCompanyName())
        .userType("PARTNER") // 협력사 타입 고정
        .level(partner.getLevel())
        .status(partner.getStatus().name())
        .passwordChanged(partner.getPasswordChanged())
        .createdAt(partner.getCreatedAt())
        .build();
  }
}