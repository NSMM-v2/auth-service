package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.entity.Partner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 협력사 트리 구조 관리 전문 서비스
 * 
 * 책임:
 * - 트리 경로 생성 및 관리
 * - 계층 관계 검증
 * - 상하위 관계 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerTreeService {

  /**
   * 트리 경로 생성
   * 
   * @param partner 협력사 엔티티
   * @return 생성된 트리 경로
   */
  public String generateTreePath(Partner partner) {
    if (partner.getParent() == null) {
      return String.format("/%d/", partner.getId());
    } else {
      return partner.getParent().getTreePath() + partner.getId() + "/";
    }
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
    return partner.getParent() == null;
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
}