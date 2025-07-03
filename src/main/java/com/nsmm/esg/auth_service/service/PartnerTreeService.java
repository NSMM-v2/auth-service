package com.nsmm.esg.auth_service.service;

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