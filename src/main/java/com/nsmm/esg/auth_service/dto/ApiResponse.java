package com.nsmm.esg.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API 응답 공통 DTO
 * 
 * 특징: 모든 API 응답의 표준 형식 제공
 * 용도: 성공/실패 응답 통일, 에러 코드 관리
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

  private boolean success; // 성공 여부
  private String message; // 응답 메시지
  private T data; // 응답 데이터
  private String errorCode; // 에러 코드 (실패 시)
  private LocalDateTime timestamp; // 응답 시간

  /**
   * 성공 응답 생성
   */
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .message("요청이 성공적으로 처리되었습니다.")
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * 성공 응답 생성 (메시지 포함)
   */
  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * 실패 응답 생성
   */
  public static <T> ApiResponse<T> error(String message) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * 실패 응답 생성 (에러 코드 포함)
   */
  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .errorCode(errorCode)
        .timestamp(LocalDateTime.now())
        .build();
  }
}