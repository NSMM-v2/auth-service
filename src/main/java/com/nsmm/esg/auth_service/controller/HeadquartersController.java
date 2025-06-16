package com.nsmm.esg.auth_service.controller;

import com.nsmm.esg.auth_service.dto.AuthDto;
import com.nsmm.esg.auth_service.dto.HeadquartersDto;
import com.nsmm.esg.auth_service.service.HeadquartersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 본사 컨트롤러 - 본사 회원가입, 로그인, 정보 관리 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/headquarters")
@RequiredArgsConstructor
@Tag(name = "본사 관리", description = "본사 회원가입, 로그인, 정보 관리 API")
public class HeadquartersController {

        private final HeadquartersService headquartersService;

        @Operation(summary = "본사 회원가입 (8자리 숫자 계정 + 친화적 비밀번호)")
        @PostMapping("/signup")
        public ResponseEntity<AuthDto.ApiResponse<HeadquartersDto.SignupResponse>> signup(
                        @Valid @RequestBody HeadquartersDto.SignupRequest request) {

                log.info("본사 회원가입 요청: {}", request.getEmail());

                try {
                        HeadquartersDto.SignupResponse response = headquartersService.signup(request);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(AuthDto.ApiResponse.success(response, "본사 회원가입이 완료되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("본사 회원가입 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "SIGNUP_FAILED"));
                } catch (Exception e) {
                        log.error("본사 회원가입 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "본사 로그인")
        @PostMapping("/login")
        public ResponseEntity<AuthDto.ApiResponse<AuthDto.TokenResponse>> login(
                        @Valid @RequestBody HeadquartersDto.LoginRequest request,
                        HttpServletResponse response) {

                log.info("본사 로그인 요청: {}", request.getEmail());

                try {
                        AuthDto.TokenResponse tokenResponse = headquartersService.login(request);

                        // JWT 토큰을 쿠키에 설정
                        setJwtCookie(response, tokenResponse.getAccessToken());

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(tokenResponse, "로그인이 완료되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("본사 로그인 실패: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "LOGIN_FAILED"));
                } catch (Exception e) {
                        log.error("본사 로그인 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "본사 정보 조회", security = @SecurityRequirement(name = "JWT"))
        @GetMapping("/{headquartersId}")
        @PreAuthorize("hasRole('HEADQUARTERS') and @securityUtil.getCurrentHeadquartersId() == #headquartersId")
        public ResponseEntity<AuthDto.ApiResponse<HeadquartersDto.Response>> getHeadquartersInfo(
                        @PathVariable Long headquartersId) {

                log.info("본사 정보 조회 요청: {}", headquartersId);

                try {
                        HeadquartersDto.Response response = headquartersService.getHeadquartersInfo(headquartersId);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (IllegalArgumentException e) {
                        log.warn("본사 정보 조회 실패: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "NOT_FOUND"));
                } catch (Exception e) {
                        log.error("본사 정보 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "본사 정보 수정", security = @SecurityRequirement(name = "JWT"))
        @PutMapping("/{headquartersId}")
        @PreAuthorize("hasRole('HEADQUARTERS') and @securityUtil.getCurrentHeadquartersId() == #headquartersId")
        public ResponseEntity<AuthDto.ApiResponse<HeadquartersDto.Response>> updateHeadquartersInfo(
                        @PathVariable Long headquartersId,
                        @Valid @RequestBody HeadquartersDto.UpdateRequest request) {

                log.info("본사 정보 수정 요청: {}", headquartersId);

                try {
                        HeadquartersDto.Response response = headquartersService.updateHeadquartersInfo(headquartersId,
                                        request);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response, "본사 정보가 수정되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("본사 정보 수정 실패: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "NOT_FOUND"));
                } catch (Exception e) {
                        log.error("본사 정보 수정 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "이메일 중복 확인")
        @GetMapping("/check-email")
        public ResponseEntity<AuthDto.ApiResponse<Boolean>> checkEmailExists(
                        @RequestParam String email) {

                log.info("이메일 중복 확인 요청: {}", email);

                try {
                        boolean exists = headquartersService.isEmailExists(email);
                        String message = exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(exists, message));
                } catch (Exception e) {
                        log.error("이메일 중복 확인 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "본사 상태 변경", security = @SecurityRequirement(name = "JWT"))
        @PatchMapping("/{headquartersId}/status")
        @PreAuthorize("hasRole('HEADQUARTERS')")
        public ResponseEntity<AuthDto.ApiResponse<Void>> changeStatus(
                        @PathVariable Long headquartersId,
                        @RequestParam String status) {

                log.info("본사 상태 변경 요청: {} -> {}", headquartersId, status);

                try {
                        // Enum 변환
                        var companyStatus = com.nsmm.esg.auth_service.entity.Headquarters.CompanyStatus
                                        .valueOf(status.toUpperCase());
                        headquartersService.changeStatus(headquartersId, companyStatus);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(null, "본사 상태가 변경되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("본사 상태 변경 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "INVALID_STATUS"));
                } catch (Exception e) {
                        log.error("본사 상태 변경 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "로그아웃", security = @SecurityRequirement(name = "JWT"))
        @PostMapping("/logout")
        @PreAuthorize("hasRole('HEADQUARTERS')")
        public ResponseEntity<AuthDto.ApiResponse<Void>> logout(HttpServletResponse response) {
                log.info("본사 로그아웃 요청");

                try {
                        // JWT 쿠키 삭제
                        clearJwtCookie(response);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(null, "로그아웃이 완료되었습니다."));
                } catch (Exception e) {
                        log.error("로그아웃 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * JWT 토큰을 HttpOnly 쿠키에 설정
         */
        private void setJwtCookie(HttpServletResponse response, String token) {
                Cookie jwtCookie = new Cookie("jwt", token);
                jwtCookie.setHttpOnly(true); // XSS 방지
                jwtCookie.setSecure(true); // HTTPS에서만 전송
                jwtCookie.setPath("/"); // 모든 경로에서 사용
                jwtCookie.setMaxAge(24 * 60 * 60); // 24시간 (초 단위)
                jwtCookie.setAttribute("SameSite", "Strict"); // CSRF 방지

                response.addCookie(jwtCookie);
                log.debug("JWT 쿠키 설정 완료");
        }

        /**
         * JWT 쿠키 삭제
         */
        private void clearJwtCookie(HttpServletResponse response) {
                Cookie jwtCookie = new Cookie("jwt", "");
                jwtCookie.setHttpOnly(true);
                jwtCookie.setSecure(true);
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(0); // 즉시 만료
                jwtCookie.setAttribute("SameSite", "Strict");

                response.addCookie(jwtCookie);
                log.debug("JWT 쿠키 삭제 완료");
        }
}