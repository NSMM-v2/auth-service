package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.repository.PartnerRepository;
import com.nsmm.esg.auth_service.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 협력사 계층적 아이디 생성 서비스
 * 
 * 새로운 방식: /{본사ID}/L{레벨}-{순번}/
 * - 1차 협력사: L1-001, L1-002, L1-003...
 * - 2차 협력사: L2-001, L2-002, L2-003...
 * - 3차 협력사: L3-001, L3-002, L3-003...
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerAccountService {

  private final PartnerRepository partnerRepository;
  private final SecurityUtil securityUtil;



  /**
   * 계층적 아이디 생성 (통합 메서드)
   * 
   * @param headquartersId  본사 ID
   * @param level           협력사 레벨 (1, 2, 3...)
   * @param parentPartnerId 상위 협력사 ID (1차 협력사인 경우 null)
   */
  public String generateHierarchicalId(Long headquartersId, int level, Long parentPartnerId) {
    log.info("계층적 아이디 생성: 본사ID={}, 레벨={}, 상위ID={}", headquartersId, level, parentPartnerId);

    // 해당 본사에서 같은 레벨의 협력사 수 조회
    long count = partnerRepository.countByHeadquartersAndLevel(headquartersId, level);
    int sequence = (int) (count + 1);

    // 중복 확인 및 조정
    String candidateId = String.format("L%d-%03d", level, sequence);

    // 본사의 계정번호 조회 필요 (임시로 시퀀스만 사용)
    while (isHierarchicalIdDuplicate(headquartersId, level, sequence)) {
      sequence++;
      candidateId = String.format("L%d-%03d", level, sequence);
    }

    log.info("계층적 아이디 생성 완료: {}", candidateId);
    return candidateId;
  }

  /**
   * 계층적 아이디 중복 확인 (헬퍼 메서드)
   */
  private boolean isHierarchicalIdDuplicate(Long headquartersId, int level, int sequence) {
    return partnerRepository.countByHeadquartersAndLevel(headquartersId, level) >= sequence;
  }
}