package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.repository.HeadquartersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 본사 계정번호 생성 서비스
 * 
 * 비즈니스 규칙:
 * - 본사 계정번호는 일자별로 고유하게 생성
 * - 형식: YYMMDD + 순번 (총 10자리)
 * - 보안을 위해 17로 시작하는 순번 사용 (예측 어려움)
 * - 일일 생성 한도: 100개 (충분한 용량)
 * 
 * 계정번호 형식:
 * - YYMMDD: 생성 날짜 (6자리)
 * - 17XX: 17로 시작하는 순번 (4자리, 1700~1799)
 * 
 * 예시: 2412161700, 2412161701, 2412161702...
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeadquartersAccountService {

  private final HeadquartersRepository headquartersRepository;

  // 계정번호 생성 규칙
  private static final int ACCOUNT_NUMBER_LENGTH = 10; // 계정번호 총 길이
  private static final int DATE_PART_LENGTH = 6; // 날짜 부분 길이 (YYMMDD)
  private static final int SEQUENCE_PREFIX = 17; // 순번 접두사 (보안)
  private static final int MIN_SEQUENCE = 1700; // 최소 순번
  private static final int MAX_SEQUENCE = 1799; // 최대 순번
  private static final int DAILY_CAPACITY = MAX_SEQUENCE - MIN_SEQUENCE + 1; // 일일 최대 생성 수

  /**
   * 새로운 본사 계정번호 생성
   * 형식: YYMMDD + 17XX (예: 2412161700)
   */
  public String generateAccountNumber() {
    log.info("새로운 본사 계정번호 생성 시작");

    // 현재 날짜 기준 기본 패턴 생성 (20 제거하여 6자리)
    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

    // 오늘 날짜로 생성된 본사 수 조회
    long todayCount = getTodayHeadquartersCount(today);

    // 순번 생성 (1700부터 시작)
    int sequence = MIN_SEQUENCE + (int) todayCount;

    // 최대 순번 확인
    if (sequence > MAX_SEQUENCE) {
      throw new IllegalStateException(
          String.format("일일 본사 생성 한도(%d개)를 초과했습니다.", DAILY_CAPACITY));
    }

    String accountNumber = String.format("%s%d", today, sequence);

    // 중복 확인 및 조정
    while (headquartersRepository.existsByHqAccountNumber(accountNumber)) {
      sequence++;
      if (sequence > MAX_SEQUENCE) {
        throw new IllegalStateException(
            String.format("일일 본사 생성 한도(%d개)를 초과했습니다.", DAILY_CAPACITY));
      }
      accountNumber = String.format("%s%d", today, sequence);
    }

    log.info("본사 계정번호 생성 완료: {}", accountNumber);
    return accountNumber;
  }

  /**
   * 오늘 날짜 기준으로 생성된 본사 수 조회
   */
  private long getTodayHeadquartersCount(String datePattern) {
    // 해당 날짜로 시작하는 계정번호를 가진 본사 수 조회
    return headquartersRepository.countByHqAccountNumberStartingWith(datePattern);
  }

  /**
   * 본사 계정번호 유효성 검증
   * 형식: YYMMDD + 17XX (총 10자리)
   */
  public boolean isValidAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.trim().isEmpty()) {
      return false;
    }

    // 계정번호 길이 검증
    if (accountNumber.length() != ACCOUNT_NUMBER_LENGTH) {
      return false;
    }

    // 형식 검증: 숫자이면서 뒤 4자리가 17로 시작하는지 확인
    String pattern = String.format("^\\d{%d}%d\\d{2}$", DATE_PART_LENGTH, SEQUENCE_PREFIX);
    boolean matches = accountNumber.matches(pattern);

    if (matches) {
      // 순번이 유효 범위인지 추가 확인
      int sequence = extractSequence(accountNumber);
      return sequence >= MIN_SEQUENCE && sequence <= MAX_SEQUENCE;
    }

    return false;
  }

  /**
   * 계정번호에서 순번 추출
   */
  public int extractSequence(String accountNumber) {
    if (accountNumber == null || accountNumber.length() != ACCOUNT_NUMBER_LENGTH) {
      throw new IllegalArgumentException("잘못된 본사 계정번호 형식: " + accountNumber);
    }

    try {
      // 2412161700 → 1700
      String sequenceStr = accountNumber.substring(DATE_PART_LENGTH);
      return Integer.parseInt(sequenceStr);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("잘못된 본사 계정번호 형식: " + accountNumber);
    }
  }

  /**
   * 다음 사용 가능한 계정번호 미리 확인
   */
  public String getNextAvailableAccountNumber() {
    try {
      return generateAccountNumber();
    } catch (Exception e) {
      log.warn("다음 계정번호 생성 실패: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 오늘 생성 가능한 남은 계정번호 수
   */
  public int getTodayRemainingCount() {
    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
    long todayCount = getTodayHeadquartersCount(today);
    return (int) Math.max(0, DAILY_CAPACITY - todayCount);
  }

}