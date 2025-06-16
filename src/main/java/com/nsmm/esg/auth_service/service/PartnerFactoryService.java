package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.dto.partner.PartnerCreateRequest;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.entity.Partner;
import com.nsmm.esg.auth_service.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 협력사 생성 전문 서비스
 * 
 * 역할:
 * - 1차 협력사 생성 (본사 직속)
 * - 하위 협력사 생성 (상위 협력사의 하위)
 * - 계층적 ID 및 트리 경로 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerFactoryService {

  private final PasswordUtil passwordUtil;
  private final PartnerAccountService partnerAccountService;
  private final PartnerTreeService partnerTreeService;

  /**
   * 1차 협력사 생성
   * 본사에서 직접 생성하는 1차 협력사
   */
  public Partner createFirstLevelPartner(Headquarters headquarters, PartnerCreateRequest request) {
    log.info("1차 협력사 생성: 본사ID={}, 회사명={}", headquarters.getId(), request.getCompanyName());

    // 계층적 ID 생성 (기존 메서드 사용)
    String hierarchicalId = partnerAccountService.generateFirstLevelId(request.getContactPerson());

    // 초기 비밀번호는 계층적 ID와 동일
    String initialPassword = hierarchicalId;
    String encodedPassword = passwordUtil.encodePassword(initialPassword);

    // 1차 협력사 엔티티 생성 (트리 경로는 임시로 설정, 저장 후 업데이트)
    return Partner.builder()
        .headquarters(headquarters)
        .parent(null) // 1차 협력사는 상위가 없음
        .hqAccountNumber(headquarters.getHqAccountNumber())
        .hierarchicalId(hierarchicalId)
        .companyName(request.getCompanyName())
        .email(request.getEmail())
        .password(encodedPassword)
        .contactPerson(request.getContactPerson())
        .phone(request.getPhone())
        .address(request.getAddress())
        .level(1)
        .treePath("/temp/") // 임시 경로, 저장 후 업데이트
        .status(Partner.PartnerStatus.ACTIVE)
        .passwordChanged(false) // 초기 상태
        .build();
  }

  /**
   * 하위 협력사 생성
   * 상위 협력사에서 하위 협력사 생성
   */
  public Partner createSubPartner(Partner parentPartner, PartnerCreateRequest request) {
    log.info("하위 협력사 생성: 상위ID={}, 회사명={}", parentPartner.getId(), request.getCompanyName());

    // 하위 레벨 계산
    int childLevel = partnerTreeService.calculateLevel(parentPartner);

    // 계층적 ID 생성 (기존 메서드 사용)
    String hierarchicalId = partnerAccountService.generateSubLevelId(
        request.getContactPerson(), childLevel, parentPartner.getId());

    // 초기 비밀번호는 계층적 ID와 동일
    String initialPassword = hierarchicalId;
    String encodedPassword = passwordUtil.encodePassword(initialPassword);

    // 하위 협력사 엔티티 생성 (트리 경로는 임시로 설정, 저장 후 업데이트)
    return Partner.builder()
        .headquarters(parentPartner.getHeadquarters())
        .parent(parentPartner)
        .hqAccountNumber(parentPartner.getHqAccountNumber())
        .hierarchicalId(hierarchicalId)
        .companyName(request.getCompanyName())
        .email(request.getEmail())
        .password(encodedPassword)
        .contactPerson(request.getContactPerson())
        .phone(request.getPhone())
        .address(request.getAddress())
        .level(childLevel)
        .treePath("/temp/") // 임시 경로, 저장 후 업데이트
        .status(Partner.PartnerStatus.ACTIVE)
        .passwordChanged(false) // 초기 상태
        .build();
  }

  /**
   * 트리 경로 업데이트
   * 협력사 저장 후 실제 ID를 사용하여 트리 경로 업데이트
   */
  public Partner updateTreePath(Partner partner) {
    String finalTreePath = partnerTreeService.generateTreePath(partner);

    return Partner.builder()
        .id(partner.getId())
        .headquarters(partner.getHeadquarters())
        .parent(partner.getParent())
        .children(partner.getChildren())
        .hqAccountNumber(partner.getHqAccountNumber())
        .hierarchicalId(partner.getHierarchicalId())
        .companyName(partner.getCompanyName())
        .email(partner.getEmail())
        .password(partner.getPassword())
        .contactPerson(partner.getContactPerson())
        .phone(partner.getPhone())
        .address(partner.getAddress())
        .level(partner.getLevel())
        .treePath(finalTreePath)
        .status(partner.getStatus())
        .passwordChanged(partner.getPasswordChanged())
        .createdAt(partner.getCreatedAt())
        .updatedAt(partner.getUpdatedAt())
        .build();
  }
}