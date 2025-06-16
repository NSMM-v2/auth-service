package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 협력사 계층적 아이디 생성 서비스
 * 
 * 주요 기능:
 * - 1차 협력사 아이디 생성 (p1-xxx01)
 * - 하위 협력사 아이디 생성 (p2-xxx01, p3-xxx01)
 * - 담당자명 → 이니셜 변환
 * - 중복 방지 순번 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerAccountService {

  private final PartnerRepository partnerRepository;

  // 본사 고정 계정 번호
  private static final String FIXED_HQ_ACCOUNT_NUMBER = "17250676";

  // 한글 → 영문 이니셜 매핑
  private static final Map<String, String> KOREAN_TO_ENGLISH = Map.ofEntries(
      Map.entry("김", "k"), Map.entry("이", "l"), Map.entry("박", "p"), Map.entry("최", "ch"),
      Map.entry("정", "j"), Map.entry("강", "ka"), Map.entry("윤", "y"), Map.entry("장", "ja"),
      Map.entry("임", "i"), Map.entry("한", "h"), Map.entry("오", "o"), Map.entry("서", "s"),
      Map.entry("신", "sh"), Map.entry("권", "kw"), Map.entry("황", "hw"), Map.entry("안", "a"),
      Map.entry("송", "so"), Map.entry("전", "je"), Map.entry("홍", "ho"), Map.entry("문", "m"),
      Map.entry("양", "ya"), Map.entry("손", "son"), Map.entry("배", "b"), Map.entry("백", "ba"),
      Map.entry("허", "he"), Map.entry("유", "yu"), Map.entry("남", "n"), Map.entry("심", "si"),
      Map.entry("노", "no"), Map.entry("곽", "g"), Map.entry("성", "se"), Map.entry("차", "c"),
      Map.entry("주", "ju"), Map.entry("우", "w"), Map.entry("구", "gu"), Map.entry("신", "shin"),
      Map.entry("조", "cho"), Map.entry("마", "ma"), Map.entry("진", "jin"), Map.entry("민", "min"),
      Map.entry("혁", "hye"), Map.entry("칠", "chil"), Map.entry("팔", "pal"));

  /**
   * 1차 협력사 계층적 아이디 생성
   * 형식: p1-xxx01 (예: p1-kcs01, p1-lyh01)
   */
  public String generateFirstLevelId(String contactPersonName) {
    log.info("1차 협력사 아이디 생성: 담당자명={}", contactPersonName);

    // 담당자명에서 이니셜 추출
    String initials = extractInitials(contactPersonName);

    // 1차 협력사는 레벨 1
    int level = 1;

    // 순번 생성
    int sequence = getNextSequence(initials, level, null);

    String hierarchicalId = String.format("p%d-%s%02d", level, initials, sequence);

    log.info("1차 협력사 아이디 생성 완료: {}", hierarchicalId);
    return hierarchicalId;
  }

  /**
   * 하위 협력사 계층적 아이디 생성
   * 형식: p2-xxx01, p3-xxx01 등 (예: p2-lyh01, p3-kcs01)
   */
  public String generateSubLevelId(String contactPersonName, int level, Long parentId) {
    log.info("하위 협력사 아이디 생성: 담당자명={}, 레벨={}, 상위ID={}", contactPersonName, level, parentId);

    // 담당자명에서 이니셜 추출
    String initials = extractInitials(contactPersonName);

    // 순번 생성 (같은 레벨, 같은 상위에서 중복 방지)
    int sequence = getNextSequence(initials, level, parentId);

    String hierarchicalId = String.format("p%d-%s%02d", level, initials, sequence);

    log.info("하위 협력사 아이디 생성 완료: {}", hierarchicalId);
    return hierarchicalId;
  }

  /**
   * 담당자명에서 이니셜 추출
   * 한글 → 영문 변환 후 첫 3자리 사용
   */
  private String extractInitials(String contactPersonName) {
    if (contactPersonName == null || contactPersonName.trim().isEmpty()) {
      throw new IllegalArgumentException("담당자명이 비어있습니다.");
    }

    String name = contactPersonName.trim();
    StringBuilder initials = new StringBuilder();

    // 각 글자를 영문으로 변환
    for (char c : name.toCharArray()) {
      String korean = String.valueOf(c);
      String english = KOREAN_TO_ENGLISH.get(korean);

      if (english != null) {
        initials.append(english);
      } else {
        // 한글이 아닌 경우 소문자로 변환하여 추가
        initials.append(String.valueOf(c).toLowerCase());
      }

      // 최대 3자리까지만
      if (initials.length() >= 3) {
        break;
      }
    }

    // 최소 2자리, 최대 3자리 보장
    String result = initials.toString();
    if (result.length() < 2) {
      result = result + "x".repeat(2 - result.length()); // 부족한 부분은 x로 채움
    } else if (result.length() > 3) {
      result = result.substring(0, 3);
    }

    log.debug("이니셜 추출: {} → {}", contactPersonName, result);
    return result;
  }

  /**
   * 다음 순번 생성
   * 같은 이니셜, 같은 레벨에서 중복되지 않는 순번 반환
   */
  private int getNextSequence(String initials, int level, Long parentId) {
    // 기존 협력사 수 조회하여 다음 순번 계산
    String basePattern = String.format("p%d-%s", level, initials);

    // 같은 본사, 같은 레벨에서 이 이니셜로 시작하는 협력사 수 조회
    long count;
    if (parentId == null) {
      // 1차 협력사의 경우
      count = partnerRepository.countByHeadquartersIdAndLevel(getDefaultHeadquartersId(), level);
    } else {
      // 하위 협력사의 경우 - 실제로는 더 정교한 로직 필요
      count = partnerRepository.countByHeadquartersIdAndLevel(getDefaultHeadquartersId(), level);
    }

    int sequence = (int) (count + 1);

    // 중복 확인 및 조정
    String candidateId = String.format("p%d-%s%02d", level, initials, sequence);
    while (partnerRepository.existsByHqAccountNumberAndHierarchicalId(FIXED_HQ_ACCOUNT_NUMBER, candidateId)) {
      sequence++;
      candidateId = String.format("p%d-%s%02d", level, initials, sequence);
    }

    log.debug("순번 생성: 이니셜={}, 레벨={}, 순번={}", initials, level, sequence);
    return sequence;
  }

  /**
   * 기본 본사 ID 반환 (임시)
   * 실제로는 컨텍스트에서 가져와야 함
   */
  private Long getDefaultHeadquartersId() {
    return 1L; // 현재는 고정값, 추후 개선 필요
  }

  /**
   * 계층적 아이디 유효성 검증
   */
  public boolean isValidHierarchicalId(String hierarchicalId) {
    if (hierarchicalId == null || hierarchicalId.trim().isEmpty()) {
      return false;
    }

    // p{레벨}-{이니셜}{순번} 형식 검증
    return hierarchicalId.matches("^p\\d+-[a-z]{2,3}\\d{2}$");
  }

  /**
   * 계층적 아이디에서 레벨 추출
   */
  public int extractLevel(String hierarchicalId) {
    if (!isValidHierarchicalId(hierarchicalId)) {
      throw new IllegalArgumentException("잘못된 계층적 아이디 형식: " + hierarchicalId);
    }

    // p1-kcs01 → 1
    String levelStr = hierarchicalId.substring(1, hierarchicalId.indexOf('-'));
    return Integer.parseInt(levelStr);
  }

  /**
   * 계층적 아이디에서 이니셜 추출
   */
  public String extractInitialsFromId(String hierarchicalId) {
    if (!isValidHierarchicalId(hierarchicalId)) {
      throw new IllegalArgumentException("잘못된 계층적 아이디 형식: " + hierarchicalId);
    }

    // p1-kcs01 → kcs
    int dashIndex = hierarchicalId.indexOf('-');
    return hierarchicalId.substring(dashIndex + 1, hierarchicalId.length() - 2);
  }
}