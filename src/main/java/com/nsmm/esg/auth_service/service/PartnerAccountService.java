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
   * 1차 협력사 계층적 아이디 생성
   * 형식: L1-001, L1-002, L1-003...
   */
  public String generateFirstLevelId() {
    log.info("1차 협력사 아이디 생성 시작");

    int level = 1;
    int sequence = getNextSequence(level, null);
    String hierarchicalId = String.format("L%d-%03d", level, sequence);

    log.info("1차 협력사 아이디 생성 완료: {}", hierarchicalId);
    return hierarchicalId;
  }

  /**
   * 하위 협력사 계층적 아이디 생성
   * 형식: L2-001, L3-001...
   */
  public String generateSubLevelId(int level, Long parentId) {
    log.info("하위 협력사 아이디 생성: 레벨={}, 상위ID={}", level, parentId);

    int sequence = getNextSequence(level, parentId);
    String hierarchicalId = String.format("L%d-%03d", level, sequence);

    log.info("하위 협력사 아이디 생성 완료: {}", hierarchicalId);
    return hierarchicalId;
  }

  /**
   * 다음 순번 생성
   * 같은 본사, 같은 레벨에서 중복되지 않는 순번 반환
   */
  private int getNextSequence(int level, Long parentId) {
    Long currentHqId = getCurrentHeadquartersId();

    // 같은 본사, 같은 레벨에서 생성된 협력사 수 조회
    long count = partnerRepository.countByHeadquartersIdAndLevel(currentHqId, level);

    int sequence = (int) (count + 1);

    // 중복 확인 및 조정
    String candidateId = String.format("L%d-%03d", level, sequence);
    String currentHqAccountNumber = getCurrentHeadquartersAccountNumber();

    while (partnerRepository.existsByHqAccountNumberAndHierarchicalId(currentHqAccountNumber, candidateId)) {
      sequence++;
      candidateId = String.format("L%d-%03d", level, sequence);
    }

    log.debug("순번 생성: 레벨={}, 순번={}", level, sequence);
    return sequence;
  }

  /**
   * 현재 로그인한 본사 ID 반환
   */
  private Long getCurrentHeadquartersId() {
    try {
      return securityUtil.getCurrentHeadquartersId();
    } catch (Exception e) {
      log.error("현재 본사 ID를 가져올 수 없습니다: {}", e.getMessage());
      throw new IllegalStateException("인증된 본사 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
    }
  }

  /**
   * 현재 로그인한 본사의 계정번호 반환
   */
  private String getCurrentHeadquartersAccountNumber() {
    try {
      return securityUtil.getCurrentAccountNumber();
    } catch (Exception e) {
      log.error("현재 본사 계정번호를 가져올 수 없습니다: {}", e.getMessage());
      throw new IllegalStateException("인증된 본사 계정번호를 찾을 수 없습니다. 다시 로그인해주세요.");
    }
  }

  /**
   * 계층적 아이디 유효성 검증
   * 형식: L{레벨}-{순번} (예: L1-001, L2-003)
   */
  public boolean isValidHierarchicalId(String hierarchicalId) {
    if (hierarchicalId == null || hierarchicalId.trim().isEmpty()) {
      return false;
    }

    // L{숫자}-{3자리숫자} 형식 검증
    return hierarchicalId.matches("^L\\d+-\\d{3}$");
  }

  /**
   * 계층적 아이디에서 레벨 추출
   * L1-001 → 1
   */
  public int extractLevel(String hierarchicalId) {
    if (!isValidHierarchicalId(hierarchicalId)) {
      throw new IllegalArgumentException("잘못된 계층적 아이디 형식: " + hierarchicalId);
    }

    String levelStr = hierarchicalId.substring(1, hierarchicalId.indexOf('-'));
    return Integer.parseInt(levelStr);
  }

  /**
   * 계층적 아이디에서 순번 추출
   * L1-001 → 1
   */
  public int extractSequence(String hierarchicalId) {
    if (!isValidHierarchicalId(hierarchicalId)) {
      throw new IllegalArgumentException("잘못된 계층적 아이디 형식: " + hierarchicalId);
    }

    int dashIndex = hierarchicalId.indexOf('-');
    String sequenceStr = hierarchicalId.substring(dashIndex + 1);
    return Integer.parseInt(sequenceStr);
  }

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