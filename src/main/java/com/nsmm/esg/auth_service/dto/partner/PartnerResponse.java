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
  private Long id;

  @Schema(description = "본사 계정번호")
  private String hqAccountNumber;

  @Schema(description = "계층적 아이디")
  private String hierarchicalId;

  @Schema(description = "전체 계정번호")
  private String fullAccountNumber;

  @Schema(description = "회사명")
  private String companyName;

  @Schema(description = "이메일")
  private String email;

  @Schema(description = "담당자명")
  private String contactPerson;

  @Schema(description = "연락처")
  private String phone;

  @Schema(description = "주소")
  private String address;

  @Schema(description = "협력사 레벨")
  private Integer level;

  @Schema(description = "트리 경로")
  private String treePath;

  @Schema(description = "상태")
  private Partner.PartnerStatus status;

  @Schema(description = "비밀번호 변경 여부")
  private Boolean passwordChanged;

  @Schema(description = "생성 일시")
  private LocalDateTime createdAt;

  @Schema(description = "수정 일시")
  private LocalDateTime updatedAt;

  // 관계 정보
  @Schema(description = "상위 협력사 ID")
  private Long parentId;

  @Schema(description = "상위 협력사 계층적 아이디")
  private String parentHierarchicalId;

  @Schema(description = "상위 협력사명")
  private String parentCompanyName;

  @Schema(description = "본사 ID")
  private Long headquartersId;

  @Schema(description = "본사명")
  private String headquartersCompanyName;

  /**
   * Entity를 Response DTO로 변환
   */
  public static PartnerResponse from(Partner partner) {
    return PartnerResponse.builder()
        .id(partner.getId())
        .hqAccountNumber(partner.getHqAccountNumber())
        .hierarchicalId(partner.getHierarchicalId())
        .fullAccountNumber(partner.getFullAccountNumber())
        .companyName(partner.getCompanyName())
        .email(partner.getEmail())
        .contactPerson(partner.getContactPerson())
        .phone(partner.getPhone())
        .address(partner.getAddress())
        .level(partner.getLevel())
        .treePath(partner.getTreePath())
        .status(partner.getStatus())
        .passwordChanged(partner.getPasswordChanged())
        .createdAt(partner.getCreatedAt())
        .updatedAt(partner.getUpdatedAt())
        .parentId(partner.getParent() != null ? partner.getParent().getId() : null)
        .parentHierarchicalId(partner.getParent() != null ? partner.getParent().getHierarchicalId() : null)
        .parentCompanyName(partner.getParent() != null ? partner.getParent().getCompanyName() : null)
        .headquartersId(partner.getHeadquarters().getId())
        .headquartersCompanyName(partner.getHeadquarters().getCompanyName())
        .build();
  }
}