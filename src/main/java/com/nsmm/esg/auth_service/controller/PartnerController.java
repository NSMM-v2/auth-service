package com.nsmm.esg.auth_service.controller;

import com.nsmm.esg.auth_service.dto.AuthDto;
import com.nsmm.esg.auth_service.dto.PartnerDto;
import com.nsmm.esg.auth_service.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 협력사 컨트롤러 - 계층적 협력사 계정 생성, 로그인, 관리 API 제공 (AWS IAM 방식)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Tag(name = "협력사 관리", description = "협력사 계정 생성, 로그인, 관리 API")
public class PartnerController {

        private final PartnerService partnerService;

        /**
         * AWS 스타일 인증 계정 생성 (다른 서비스에서 호출)
         */
        @Operation(summary = "AWS 스타일 인증 계정 생성 (서비스 간 호출)")
        @PostMapping("/create-auth-account")
        public ResponseEntity<AuthDto.ApiResponse<PartnerDto.AuthAccountCreateResponse>> createAuthAccount(
                        @Valid @RequestBody PartnerDto.AuthAccountCreateRequest request) {

                log.info("AWS 스타일 인증 계정 생성 요청: 파트너ID={}, 회사명={}",
                                request.getPartnerId(), request.getCompanyName());

                try {
                        PartnerDto.AuthAccountCreateResponse response = partnerService.createAuthAccount(request);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(AuthDto.ApiResponse.success(response, "AWS 스타일 인증 계정이 생성되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("AWS 스타일 인증 계정 생성 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "CREATE_FAILED"));
                } catch (Exception e) {
                        log.error("AWS 스타일 인증 계정 생성 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "협력사 계정 생성 (기존 방식)", security = @SecurityRequirement(name = "JWT"))
        @PostMapping("/create")
        public ResponseEntity<AuthDto.ApiResponse<PartnerDto.CreateResponse>> createPartner(
                        @RequestHeader("X-Creator-Headquarters-Id") Long creatorHeadquartersId,
                        @RequestHeader(value = "X-Creator-Partner-Id", required = false) Long creatorPartnerId,
                        @Valid @RequestBody PartnerDto.CreateRequest request) {

                log.info("협력사 계정 생성 요청: {} (생성자 본사: {}, 생성자 협력사: {})",
                                request.getCompanyName(), creatorHeadquartersId, creatorPartnerId);

                try {
                        PartnerDto.CreateResponse response = partnerService.createPartner(
                                        creatorHeadquartersId, creatorPartnerId, request);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(AuthDto.ApiResponse.success(response, "협력사 계정이 생성되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("협력사 계정 생성 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "CREATE_FAILED"));
                } catch (Exception e) {
                        log.error("협력사 계정 생성 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "협력사 로그인 (AWS 스타일 + 기존 방식 지원)")
        @PostMapping("/login")
        public ResponseEntity<AuthDto.ApiResponse<AuthDto.TokenResponse>> login(
                        @Valid @RequestBody PartnerDto.LoginRequest request,
                        HttpServletResponse response) {

                log.info("협력사 로그인 요청: {}", request.getAccountNumber());

                try {
                        AuthDto.TokenResponse tokenResponse = partnerService.loginWithAwsStyle(request);

                        // JWT 토큰을 쿠키에 설정
                        setJwtCookie(response, tokenResponse.getAccessToken());

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(tokenResponse, "로그인이 완료되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("협력사 로그인 실패: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "LOGIN_FAILED"));
                } catch (Exception e) {
                        log.error("협력사 로그인 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "협력사 정보 조회", security = @SecurityRequirement(name = "JWT"))
        @GetMapping("/{partnerId}")
        public ResponseEntity<AuthDto.ApiResponse<PartnerDto.Response>> getPartnerInfo(
                        @PathVariable Long partnerId) {

                log.info("협력사 정보 조회 요청: {}", partnerId);

                try {
                        PartnerDto.Response response = partnerService.getPartnerInfo(partnerId);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (IllegalArgumentException e) {
                        log.warn("협력사 정보 조회 실패: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "NOT_FOUND"));
                } catch (Exception e) {
                        log.error("협력사 정보 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "1차 협력사 목록 조회", security = @SecurityRequirement(name = "JWT"))
        @GetMapping("/headquarters/{headquartersId}/top-level")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerDto.Response>>> getTopLevelPartners(
                        @PathVariable Long headquartersId) {

                log.info("1차 협력사 목록 조회 요청: 본사 ID {}", headquartersId);

                try {
                        List<PartnerDto.Response> response = partnerService.getTopLevelPartners(headquartersId);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (Exception e) {
                        log.error("1차 협력사 목록 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "직접 하위 협력사 목록 조회", security = @SecurityRequirement(name = "JWT"))
        @GetMapping("/{parentId}/children")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerDto.Response>>> getDirectChildren(
                        @PathVariable Long parentId) {

                log.info("직접 하위 협력사 목록 조회 요청: 상위 협력사 ID {}", parentId);

                try {
                        List<PartnerDto.Response> response = partnerService.getDirectChildren(parentId);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (Exception e) {
                        log.error("직접 하위 협력사 목록 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "하위 트리 구조 조회", security = @SecurityRequirement(name = "JWT"))
        @GetMapping("/{partnerId}/subtree")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerDto.TreeResponse>>> getSubTree(
                        @PathVariable Long partnerId) {

                log.info("하위 트리 구조 조회 요청: 협력사 ID {}", partnerId);

                try {
                        List<PartnerDto.TreeResponse> response = partnerService.getSubTree(partnerId);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (IllegalArgumentException e) {
                        log.warn("하위 트리 구조 조회 실패: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "NOT_FOUND"));
                } catch (Exception e) {
                        log.error("하위 트리 구조 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "협력사 정보 수정", security = @SecurityRequirement(name = "JWT"))
        @PutMapping("/{partnerId}")
        public ResponseEntity<AuthDto.ApiResponse<PartnerDto.Response>> updatePartnerInfo(
                        @PathVariable Long partnerId,
                        @Valid @RequestBody PartnerDto.UpdateRequest request) {

                log.info("협력사 정보 수정 요청: {}", partnerId);

                try {
                        PartnerDto.Response response = partnerService.updatePartnerInfo(partnerId, request);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response, "협력사 정보가 수정되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("협력사 정보 수정 실패: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "NOT_FOUND"));
                } catch (Exception e) {
                        log.error("협력사 정보 수정 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "비밀번호 변경", security = @SecurityRequirement(name = "JWT"))
        @PatchMapping("/{partnerId}/password")
        public ResponseEntity<AuthDto.ApiResponse<Void>> changePassword(
                        @PathVariable Long partnerId,
                        @Valid @RequestBody PartnerDto.PasswordChangeRequest request) {

                log.info("협력사 비밀번호 변경 요청: {}", partnerId);

                try {
                        partnerService.changePassword(partnerId, request);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(null, "비밀번호가 변경되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("협력사 비밀번호 변경 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "PASSWORD_CHANGE_FAILED"));
                } catch (Exception e) {
                        log.error("협력사 비밀번호 변경 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "협력사 상태 변경", security = @SecurityRequirement(name = "JWT"))
        @PatchMapping("/{partnerId}/status")
        public ResponseEntity<AuthDto.ApiResponse<Void>> changeStatus(
                        @PathVariable Long partnerId,
                        @Valid @RequestBody PartnerDto.StatusChangeRequest request) {

                log.info("협력사 상태 변경 요청: {} -> {}", partnerId, request.getStatus());

                try {
                        partnerService.changeStatus(partnerId, request);
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(null, "협력사 상태가 변경되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("협력사 상태 변경 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "STATUS_CHANGE_FAILED"));
                } catch (Exception e) {
                        log.error("협력사 상태 변경 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        @Operation(summary = "비밀번호 변경 필요 협력사 목록 조회", security = @SecurityRequirement(name = "JWT"))
        @GetMapping("/password-change-required")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerDto.Response>>> getPartnersNeedingPasswordChange() {

                log.info("비밀번호 변경 필요 협력사 목록 조회 요청");

                try {
                        List<PartnerDto.Response> response = partnerService.getPartnersNeedingPasswordChange();
                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (Exception e) {
                        log.error("비밀번호 변경 필요 협력사 목록 조회 중 오류 발생", e);
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
}