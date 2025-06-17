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
 * 새로운 방식:
 * - 계층적 ID: L{레벨}-{순번} (L1-001, L2-001...)
 * - 트리 경로: /{본사ID}/L{레벨}-{순번}/ (/{본사ID}/L1-001/L2-001/...)
 * - 초기 비밀번호: 계층적 ID와 동일
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

    // 계층적 ID 생성 (L1-001, L1-002...)
    String hierarchicalId = partnerAccountService.generateFirstLevelId();

    // 초기 비밀번호는 계층적 ID와 동일
    String initialPassword = hierarchicalId;
    String encodedPassword = passwordUtil.encodePassword(initialPassword);

    // 1차 협력사 트리 경로 생성 (/{본사ID}/L1-001/)
    String treePath = partnerTreeService.generateFirstLevelTreePath(headquarters.getId(), hierarchicalId);

    // 1차 협력사 엔티티 생성
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
        .treePath(treePath)
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

    // 계층적 ID 생성 (L2-001, L3-001...)
    String hierarchicalId = partnerAccountService.generateSubLevelId(childLevel, parentPartner.getId());

    // 초기 비밀번호는 계층적 ID와 동일
    String initialPassword = hierarchicalId;
    String encodedPassword = passwordUtil.encodePassword(initialPassword);

    // 하위 협력사 트리 경로 생성 (/{본사ID}/L1-001/L2-001/)
    String treePath = partnerTreeService.generateSubLevelTreePath(parentPartner, hierarchicalId);

    // 하위 협력사 엔티티 생성
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
        .treePath(treePath)
        .status(Partner.PartnerStatus.ACTIVE)
        .passwordChanged(false) // 초기 상태
        .build();
  }

}