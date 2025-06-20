package com.nsmm.esg.auth_service.config;

import com.nsmm.esg.auth_service.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 * Validation 에러를 한국어로 변환하여 프론트엔드에 전달
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Validation 에러 처리 (@Valid 실패 시)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    log.warn("Validation 에러 발생: {}", ex.getMessage());

    Map<String, String> errors = new HashMap<>();

    // 첫 번째 에러 메시지만 추출 (사용자 경험 개선)
    String firstErrorMessage = null;

    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      String fieldName = error.getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);

      // 첫 번째 에러를 메인 메시지로 사용
      if (firstErrorMessage == null) {
        firstErrorMessage = errorMessage;
      }
    }

    // 사용자 친화적인 메시지 반환
    String userMessage = firstErrorMessage != null ? firstErrorMessage : "입력된 정보를 확인해주세요";

    log.warn("Validation 상세 에러: {}", errors);

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(userMessage, "VALIDATION_FAILED"));
  }

  /**
   * 일반적인 IllegalArgumentException 처리
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
      IllegalArgumentException ex) {

    log.warn("잘못된 요청: {}", ex.getMessage());

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(ex.getMessage(), "BAD_REQUEST"));
  }

  /**
   * 일반적인 RuntimeException 처리
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<Object>> handleRuntimeException(
      RuntimeException ex) {

    log.error("서버 오류 발생", ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", "INTERNAL_ERROR"));
  }
}