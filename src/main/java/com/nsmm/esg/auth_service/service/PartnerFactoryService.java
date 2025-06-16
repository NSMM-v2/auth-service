package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.entity.Partner;
import com.nsmm.esg.auth_service.entity.Headquarters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 협력사 엔티티 생성 전문 서비스 (Factory Pattern)
 * 
 * 책임:
 * - 협력사 엔티티 생성
 * - 불변성 보장된 업데이트 메서드
 * - 상태 변경 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerFactoryService {

  /**
   * 새로운 협력사 엔티티 생성
   * 
   * @param headquarters         소속 본사
   * @param parent               상위 협력사
   * @param accountNumber        계층적 계정 번호
   * @param numericAccountNumber 8자리 숫자 계정
   * @param companyName          회사명
   * @param email                이메일
   * @param password             암호화된 비밀번호
   * @param contactPerson        담당자명
   * @param phone                연락처
   * @param address              주소
   * @param level                협력사 레벨
   * @param treePath             트리 경로
   * @param temporaryPassword    임시 비밀번호
   * @return 생성된 협력사 엔티티
   */
  public Partner createNewPartner(
      Headquarters headquarters,
      Partner parent,
      String accountNumber,
      String numericAccountNumber,
      String companyName,
      String email,
      String password,
      String contactPerson,
      String phone,
      String address,
      int level,
      String treePath,
      String temporaryPassword) {

    return Partner.builder()
        .headquarters(headquarters)
        .parent(parent)
        .accountNumber(accountNumber)
        .numericAccountNumber(numericAccountNumber)
        .companyName(companyName)
        .email(email)
        .password(password)
        .contactPerson(contactPerson)
        .phone(phone)
        .address(address)
        .level(level)
        .treePath(treePath)
        .status(Partner.PartnerStatus.ACTIVE)
        .passwordChanged(false)
        .temporaryPassword(temporaryPassword)
        .build();
  }

  /**
   * 협력사 정보 업데이트 (불변성 보장)
   * 
   * @param original      원본 협력사
   * @param companyName   회사명
   * @param contactPerson 담당자명
   * @param phone         연락처
   * @param address       주소
   * @return 업데이트된 새 협력사 인스턴스
   */
  public Partner updatePartnerInfo(Partner original, String companyName,
      String contactPerson, String phone, String address) {
    return Partner.builder()
        .id(original.getId())
        .headquarters(original.getHeadquarters())
        .parent(original.getParent())
        .children(original.getChildren())
        .accountNumber(original.getAccountNumber())
        .externalPartnerId(original.getExternalPartnerId())
        .numericAccountNumber(original.getNumericAccountNumber())
        .companyName(companyName != null ? companyName : original.getCompanyName())
        .email(original.getEmail())
        .password(original.getPassword())
        .contactPerson(contactPerson != null ? contactPerson : original.getContactPerson())
        .phone(phone != null ? phone : original.getPhone())
        .address(address != null ? address : original.getAddress())
        .level(original.getLevel())
        .treePath(original.getTreePath())
        .status(original.getStatus())
        .passwordChanged(original.getPasswordChanged())
        .temporaryPassword(original.getTemporaryPassword())
        .createdAt(original.getCreatedAt())
        .updatedAt(original.getUpdatedAt())
        .build();
  }

  /**
   * 비밀번호 변경 (불변성 보장)
   * 
   * @param original    원본 협력사
   * @param newPassword 새 비밀번호 (암호화된 상태)
   * @return 비밀번호가 변경된 새 협력사 인스턴스
   */
  public Partner changePassword(Partner original, String newPassword) {
    return Partner.builder()
        .id(original.getId())
        .headquarters(original.getHeadquarters())
        .parent(original.getParent())
        .children(original.getChildren())
        .accountNumber(original.getAccountNumber())
        .externalPartnerId(original.getExternalPartnerId())
        .numericAccountNumber(original.getNumericAccountNumber())
        .companyName(original.getCompanyName())
        .email(original.getEmail())
        .password(newPassword)
        .contactPerson(original.getContactPerson())
        .phone(original.getPhone())
        .address(original.getAddress())
        .level(original.getLevel())
        .treePath(original.getTreePath())
        .status(original.getStatus())
        .passwordChanged(true)
        .temporaryPassword(null) // 임시 비밀번호 제거
        .createdAt(original.getCreatedAt())
        .updatedAt(original.getUpdatedAt())
        .build();
  }

  /**
   * 상태 변경 (불변성 보장)
   * 
   * @param original  원본 협력사
   * @param newStatus 새로운 상태
   * @return 상태가 변경된 새 협력사 인스턴스
   */
  public Partner changeStatus(Partner original, Partner.PartnerStatus newStatus) {
    return Partner.builder()
        .id(original.getId())
        .headquarters(original.getHeadquarters())
        .parent(original.getParent())
        .children(original.getChildren())
        .accountNumber(original.getAccountNumber())
        .externalPartnerId(original.getExternalPartnerId())
        .numericAccountNumber(original.getNumericAccountNumber())
        .companyName(original.getCompanyName())
        .email(original.getEmail())
        .password(original.getPassword())
        .contactPerson(original.getContactPerson())
        .phone(original.getPhone())
        .address(original.getAddress())
        .level(original.getLevel())
        .treePath(original.getTreePath())
        .status(newStatus)
        .passwordChanged(original.getPasswordChanged())
        .temporaryPassword(original.getTemporaryPassword())
        .createdAt(original.getCreatedAt())
        .updatedAt(original.getUpdatedAt())
        .build();
  }

  /**
   * 계정 번호와 트리 경로 설정 (생성 시에만 사용)
   * 
   * @param original      원본 협력사
   * @param accountNumber 계층적 계정 번호
   * @param treePath      트리 경로
   * @return 계정 번호와 트리 경로가 설정된 새 협력사 인스턴스
   */
  public Partner withAccountNumberAndTreePath(Partner original, String accountNumber, String treePath) {
    return Partner.builder()
        .id(original.getId())
        .headquarters(original.getHeadquarters())
        .parent(original.getParent())
        .children(original.getChildren())
        .accountNumber(accountNumber)
        .externalPartnerId(original.getExternalPartnerId())
        .numericAccountNumber(original.getNumericAccountNumber())
        .companyName(original.getCompanyName())
        .email(original.getEmail())
        .password(original.getPassword())
        .contactPerson(original.getContactPerson())
        .phone(original.getPhone())
        .address(original.getAddress())
        .level(original.getLevel())
        .treePath(treePath)
        .status(original.getStatus())
        .passwordChanged(original.getPasswordChanged())
        .temporaryPassword(original.getTemporaryPassword())
        .createdAt(original.getCreatedAt())
        .updatedAt(original.getUpdatedAt())
        .build();
  }

  /**
   * 협력사 활성화 (생성 후 바로 활성화)
   * 
   * @param original 원본 협력사
   * @return 활성화된 새 협력사 인스턴스
   */
  public Partner activate(Partner original) {
    return Partner.builder()
        .id(original.getId())
        .headquarters(original.getHeadquarters())
        .parent(original.getParent())
        .children(original.getChildren())
        .accountNumber(original.getAccountNumber())
        .externalPartnerId(original.getExternalPartnerId())
        .numericAccountNumber(original.getNumericAccountNumber())
        .companyName(original.getCompanyName())
        .email(original.getEmail())
        .password(original.getPassword())
        .contactPerson(original.getContactPerson())
        .phone(original.getPhone())
        .address(original.getAddress())
        .level(original.getLevel())
        .treePath(original.getTreePath())
        .status(Partner.PartnerStatus.ACTIVE)
        .passwordChanged(original.getPasswordChanged())
        .temporaryPassword(original.getTemporaryPassword())
        .createdAt(original.getCreatedAt())
        .updatedAt(original.getUpdatedAt())
        .build();
  }
}