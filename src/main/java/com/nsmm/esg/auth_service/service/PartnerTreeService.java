package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.entity.Partner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 협력사 트리 구조 관리 전문 서비스
 * 
 * 새로운 트리 경로 방식: /{본사ID}/L{레벨}-{순번}/
 * - 1차 협력사: /1/L1-001/, /1/L1-002/...
 * - 2차 협력사: /1/L1-001/L2-001/, /1/L1-001/L2-002/...
 * - 3차 협력사: /1/L1-001/L2-001/L3-001/...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerTreeService {

  /**
   * 1차 협력사 트리 경로 생성
   * 형식: /{본사ID}/L{레벨}-{순번}/
   * 
   * @param headquartersId 본사 ID
   * @param hierarchicalId 계층적 아이디 (L1-001, L1-002...)
   * @return 1차 협력사용 트리 경로
   */
  public String generateFirstLevelTreePath(Long headquartersId, String hierarchicalId) {
    return String.format("/%d/%s/", headquartersId, hierarchicalId);
  }

  /**
   * 하위 협력사 트리 경로 생성
   * 상위 경로에 현재 계층적 아이디 추가
   * 
   * @param parentPartner  상위 협력사
   * @param hierarchicalId 현재 협력사의 계층적 아이디
   * @return 하위 협력사용 트리 경로
   */
  public String generateSubLevelTreePath(Partner parentPartner, String hierarchicalId) {
    return parentPartner.getTreePath() + hierarchicalId + "/";
  }

  /**
   * 하위 협력사인지 확인
   * 
   * @param child    하위로 확인할 협력사
   * @param ancestor 상위 협력사
   * @return 하위 협력사 여부
   */
  public boolean isDescendantOf(Partner child, Partner ancestor) {
    return child.getTreePath().startsWith(ancestor.getTreePath());
  }

  /**
   * 최상위 협력사인지 확인 (1차 협력사)
   * 
   * @param partner 확인할 협력사
   * @return 1차 협력사 여부
   */
  public boolean isTopLevel(Partner partner) {
    return partner.getParentPartner() == null;
  }

  /**
   * 계층 레벨 계산
   * 
   * @param parent 상위 협력사 (null이면 1차 협력사)
   * @return 계층 레벨
   */
  public int calculateLevel(Partner parent) {
    return (parent == null) ? 1 : parent.getLevel() + 1;
  }

  /**
   * 트리 경로에서 본사 ID 추출
   * /1/L1-001/ → 1
   */
  public Long extractHeadquartersIdFromTreePath(String treePath) {
    if (treePath == null || !treePath.startsWith("/")) {
      throw new IllegalArgumentException("잘못된 트리 경로 형식: " + treePath);
    }

    int firstSlash = treePath.indexOf('/', 1);
    if (firstSlash == -1) {
      throw new IllegalArgumentException("잘못된 트리 경로 형식: " + treePath);
    }

    String hqIdStr = treePath.substring(1, firstSlash);
    return Long.parseLong(hqIdStr);
  }

  /**
   * 트리 경로 유효성 검증
   * 형식: /{본사ID}/L{레벨}-{순번}/.../
   */
  public boolean isValidTreePath(String treePath) {
    if (treePath == null || treePath.trim().isEmpty()) {
      return false;
    }

    // 기본 형식 검증: /숫자/L숫자-숫자/.../
    return treePath.matches("^/\\d+(/L\\d+-\\d{3})+/$");
  }

  /**
   * 트리 경로 생성 (통합 메서드)
   * 
   * @param hqAccountNumber 본사 계정번호
   * @param hierarchicalId  협력사 계층적 ID
   * @param parentTreePath  상위 협력사의 트리 경로 (1차 협력사인 경우 null)
   * @return 생성된 트리 경로
   */
  public String generateTreePath(String hqAccountNumber, String hierarchicalId, String parentTreePath) {
    if (parentTreePath == null) {
      // 1차 협력사: /{본사계정번호}/L1-001/
      return String.format("/%s/%s/", hqAccountNumber, hierarchicalId);
    } else {
      // 하위 협력사: 상위 경로 + 현재 계층적 ID/
      return parentTreePath + hierarchicalId + "/";
    }
  }
}