package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

/**
 * 협력사 계정 번호 생성 전문 서비스
 * 
 * 책임:
 * - 계층적 아이디 생성 (p1-kcs01, p2-lyh01)
 * - 8자리 숫자 계정 번호 생성
 * - 이니셜 추출 및 매핑
 * - 중복 검사 및 순번 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerAccountService {

  private final PartnerRepository partnerRepository;

  // 한글 성씨 매핑 테이블
  private static final Map<String, String> SURNAME_MAP = Map.ofEntries(
      Map.entry("김", "k"), Map.entry("이", "l"), Map.entry("박", "p"),
      Map.entry("최", "c"), Map.entry("정", "j"), Map.entry("강", "k"),
      Map.entry("조", "j"), Map.entry("윤", "y"), Map.entry("장", "j"),
      Map.entry("임", "l"), Map.entry("한", "h"), Map.entry("오", "o"),
      Map.entry("서", "s"), Map.entry("신", "s"), Map.entry("권", "k"),
      Map.entry("황", "h"), Map.entry("안", "a"), Map.entry("송", "s"),
      Map.entry("류", "r"), Map.entry("전", "j"), Map.entry("홍", "h"),
      Map.entry("고", "g"), Map.entry("문", "m"), Map.entry("양", "y"),
      Map.entry("손", "s"), Map.entry("배", "b"), Map.entry("조", "c"),
      Map.entry("백", "b"), Map.entry("허", "h"), Map.entry("유", "y"),
      Map.entry("남", "n"), Map.entry("심", "s"), Map.entry("노", "n"),
      Map.entry("하", "h"), Map.entry("곽", "k"), Map.entry("성", "s"),
      Map.entry("차", "c"), Map.entry("주", "j"), Map.entry("우", "w"),
      Map.entry("구", "k"), Map.entry("나", "n"), Map.entry("민", "m"),
      Map.entry("진", "j"), Map.entry("지", "j"), Map.entry("엄", "e"),
      Map.entry("채", "c"), Map.entry("원", "w"), Map.entry("천", "c"),
      Map.entry("방", "b"), Map.entry("공", "k"), Map.entry("현", "h"),
      Map.entry("함", "h"), Map.entry("변", "b"), Map.entry("염", "y"),
      Map.entry("여", "y"), Map.entry("추", "c"), Map.entry("도", "d"),
      Map.entry("소", "s"), Map.entry("석", "s"), Map.entry("선", "s"),
      Map.entry("설", "s"), Map.entry("마", "m"), Map.entry("길", "g"),
      Map.entry("연", "y"), Map.entry("위", "w"), Map.entry("표", "p"),
      Map.entry("명", "m"), Map.entry("기", "k"), Map.entry("반", "b"),
      Map.entry("왕", "w"), Map.entry("금", "k"), Map.entry("옥", "o"),
      Map.entry("육", "y"), Map.entry("인", "i"), Map.entry("맹", "m"),
      Map.entry("제", "j"), Map.entry("모", "m"), Map.entry("탁", "t"),
      Map.entry("국", "k"), Map.entry("어", "e"), Map.entry("은", "e"),
      Map.entry("편", "p"), Map.entry("용", "y"));

  // 한글 이름 글자 매핑 테이블
  private static final Map<String, String> NAME_MAP = Map.ofEntries(
      Map.entry("철", "c"), Map.entry("수", "s"), Map.entry("영", "y"),
      Map.entry("희", "h"), Map.entry("민", "m"), Map.entry("호", "h"),
      Map.entry("준", "j"), Map.entry("혁", "h"), Map.entry("진", "j"),
      Map.entry("우", "w"), Map.entry("현", "h"), Map.entry("석", "s"),
      Map.entry("규", "k"), Map.entry("용", "y"), Map.entry("성", "s"),
      Map.entry("원", "w"), Map.entry("택", "t"), Map.entry("빈", "b"),
      Map.entry("환", "h"), Map.entry("식", "s"), Map.entry("동", "d"),
      Map.entry("구", "g"), Map.entry("섭", "s"), Map.entry("윤", "y"),
      Map.entry("형", "h"), Map.entry("건", "k"), Map.entry("태", "t"),
      Map.entry("완", "w"), Map.entry("균", "k"), Map.entry("훈", "h"),
      Map.entry("정", "j"), Map.entry("욱", "w"), Map.entry("길", "g"),
      Map.entry("범", "b"), Map.entry("엽", "y"), Map.entry("근", "k"),
      Map.entry("배", "b"), Map.entry("복", "b"), Map.entry("상", "s"),
      Map.entry("국", "g"), Map.entry("권", "k"), Map.entry("혁", "r"),
      Map.entry("순", "s"), Map.entry("신", "s"), Map.entry("덕", "d"),
      Map.entry("광", "k"), Map.entry("운", "u"), Map.entry("승", "s"),
      Map.entry("재", "j"), Map.entry("렬", "r"), Map.entry("무", "m"),
      Map.entry("열", "y"), Map.entry("천", "c"), Map.entry("종", "j"),
      Map.entry("립", "l"), Map.entry("관", "k"), Map.entry("칠", "c"),
      Map.entry("팔", "p"), Map.entry("십", "s"), Map.entry("일", "i"),
      Map.entry("이", "i"), Map.entry("삼", "s"), Map.entry("사", "s"),
      Map.entry("오", "o"), Map.entry("육", "y"), Map.entry("칠", "c"),
      Map.entry("팔", "p"), Map.entry("구", "k"), Map.entry("십", "s"));

  /**
   * 계층적 아이디 생성
   * 
   * @param contactPersonName 담당자명
   * @param level             협력사 레벨
   * @param parentId          상위 협력사 ID (선택적)
   * @return 생성된 계층적 아이디
   */
  public String generateHierarchicalId(String contactPersonName, int level, Long parentId) {
    String initials = extractContactInitials(contactPersonName);
    String basePattern = String.format("p%d-%s", level, initials);

    // 중복 검사를 위한 패턴
    String searchPattern = basePattern + "%";

    // 동일 이니셜의 다음 순번 조회
    Integer nextSequence = partnerRepository.getNextSequenceForInitials(
        searchPattern, level, parentId);

    String hierarchicalId = String.format("%s%02d", basePattern, nextSequence);

    log.debug("계층적 아이디 생성: 담당자={}, 레벨={}, 결과={}",
        contactPersonName, level, hierarchicalId);

    return hierarchicalId;
  }

  /**
   * 8자리 숫자 계정 번호 생성 (중복 체크 포함)
   * 
   * @return 유니크한 8자리 숫자 계정 번호
   */
  public String generateUniqueNumericAccountNumber() {
    String accountNumber;
    int attempts = 0;

    do {
      accountNumber = generateNumericAccountNumber();
      attempts++;

      if (attempts > 10) {
        throw new RuntimeException("8자리 계정 번호 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } while (partnerRepository.existsByNumericAccountNumber(accountNumber));

    return accountNumber;
  }

  /**
   * 담당자 이름에서 이니셜 추출
   * 
   * @param contactPersonName 담당자명
   * @return 3자리 이니셜
   */
  private String extractContactInitials(String contactPersonName) {
    if (contactPersonName == null || contactPersonName.trim().isEmpty()) {
      return "xxx";
    }

    String name = contactPersonName.trim();

    // 한글 이름 처리
    if (name.matches(".*[가-힣].*")) {
      return extractKoreanInitials(name);
    }

    // 영문 이름 처리
    if (name.matches(".*[a-zA-Z].*")) {
      return extractEnglishInitials(name);
    }

    return "xxx";
  }

  /**
   * 한글 이름에서 이니셜 추출
   * 
   * @param koreanName 한글 이름
   * @return 3자리 이니셜
   */
  private String extractKoreanInitials(String koreanName) {
    if (koreanName.length() >= 2) {
      String surname = koreanName.substring(0, 1);
      String firstName = koreanName.substring(1);

      StringBuilder initials = new StringBuilder();

      // 성 이니셜 추가
      initials.append(SURNAME_MAP.getOrDefault(surname, "x"));

      // 이름 이니셜 추가 (최대 2글자)
      for (int i = 0; i < Math.min(2, firstName.length()); i++) {
        String nameChar = firstName.substring(i, i + 1);
        initials.append(NAME_MAP.getOrDefault(nameChar, nameChar.toLowerCase()));
      }

      return initials.toString();
    }

    return "xxx";
  }

  /**
   * 영문 이름에서 이니셜 추출
   * 
   * @param englishName 영문 이름
   * @return 3자리 이니셜
   */
  private String extractEnglishInitials(String englishName) {
    String[] parts = englishName.toLowerCase().split("\\s+");
    StringBuilder initials = new StringBuilder();

    for (String part : parts) {
      if (!part.isEmpty()) {
        initials.append(part.charAt(0));
      }
    }

    // 3자리로 맞추기 (부족하면 x로 채움)
    while (initials.length() < 3) {
      initials.append("x");
    }

    return initials.length() > 3 ? initials.substring(0, 3) : initials.toString();
  }

  /**
   * 8자리 숫자 계정 번호 생성
   * 
   * @return 8자리 숫자 문자열
   */
  private String generateNumericAccountNumber() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    int accountNumber = random.nextInt(10000000, 100000000);
    return String.valueOf(accountNumber);
  }
}