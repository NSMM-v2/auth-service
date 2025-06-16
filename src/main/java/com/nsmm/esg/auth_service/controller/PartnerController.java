package com.nsmm.esg.auth_service.controller;

import com.nsmm.esg.auth_service.dto.AuthDto;
import com.nsmm.esg.auth_service.dto.partner.PartnerCreateRequest;
import com.nsmm.esg.auth_service.dto.partner.PartnerCreateResponse;
import com.nsmm.esg.auth_service.dto.partner.PartnerLoginRequest;
import com.nsmm.esg.auth_service.dto.partner.PartnerResponse;
import com.nsmm.esg.auth_service.dto.partner.PartnerInitialPasswordChangeRequest;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.entity.Partner;
import com.nsmm.esg.auth_service.service.HeadquartersService;
import com.nsmm.esg.auth_service.service.PartnerService;
import com.nsmm.esg.auth_service.util.JwtUtil;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 협력사 관리 컨트롤러
 * 
 * 주요 기능:
 * - 협력사 생성 (1차, 하위)
 * - 계층적 로그인 (본사계정번호 + 계층적아이디 + 비밀번호)
 * - 협력사 정보 관리
 * - 초기 비밀번호 변경
 */
@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "협력사 관리", description = "계층형 협력사 생성, 로그인, 관리 API")
public class PartnerController {

        private final PartnerService partnerService;
        private final HeadquartersService headquartersService;
        private final JwtUtil jwtUtil;

        /**
         * 1차 협력사 생성 (본사에서 생성)
         */
        @PostMapping("/first-level")
        @Operation(summary = "1차 협력사 생성", description = "본사에서 1차 협력사를 생성합니다")
        @PreAuthorize("hasRole('HEADQUARTERS')")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<PartnerCreateResponse>> createFirstLevelPartner(
                        @RequestHeader("X-Headquarters-Id") Long headquartersId,
                        @Valid @RequestBody PartnerCreateRequest request) {

                log.info("1차 협력사 생성 요청: 본사ID={}, 회사명={}", headquartersId, request.getCompanyName());

                try {
                        // 본사 조회
                        Headquarters headquarters = headquartersService.findById(headquartersId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 본사입니다: " + headquartersId));

                        // 1차 협력사 생성
                        Partner partner = partnerService.createFirstLevelPartner(headquarters, request);
                        PartnerCreateResponse response = PartnerCreateResponse.from(partner);

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(AuthDto.ApiResponse.success(response, "1차 협력사가 성공적으로 생성되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("1차 협력사 생성 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "CREATE_FAILED"));
                } catch (Exception e) {
                        log.error("1차 협력사 생성 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 하위 협력사 생성 (상위 협력사에서 생성)
         */
        @PostMapping("/{parentId}/sub-partners")
        @Operation(summary = "하위 협력사 생성", description = "상위 협력사에서 하위 협력사를 생성합니다")
        @PreAuthorize("hasRole('PARTNER')")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<PartnerCreateResponse>> createSubPartner(
                        @PathVariable Long parentId,
                        @Valid @RequestBody PartnerCreateRequest request) {

                log.info("하위 협력사 생성 요청: 상위ID={}, 회사명={}", parentId, request.getCompanyName());

                try {
                        // 상위 협력사 조회
                        Partner parentPartner = partnerService.findById(parentId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 상위 협력사입니다: " + parentId));

                        // 하위 협력사 생성
                        Partner partner = partnerService.createSubPartner(parentPartner, request);
                        PartnerCreateResponse response = PartnerCreateResponse.from(partner);

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(AuthDto.ApiResponse.success(response, "하위 협력사가 성공적으로 생성되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("하위 협력사 생성 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "CREATE_FAILED"));
                } catch (Exception e) {
                        log.error("하위 협력사 생성 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 협력사 로그인
         * 본사계정번호 + 계층적아이디 + 비밀번호
         */
        @PostMapping("/login")
        @Operation(summary = "협력사 로그인", description = "본사계정번호 + 계층적아이디 + 비밀번호로 로그인")
        public ResponseEntity<AuthDto.ApiResponse<AuthDto.TokenResponse>> login(
                        @Valid @RequestBody PartnerLoginRequest request,
                        HttpServletResponse response) {

                log.info("협력사 로그인 요청: 본사계정번호={}, 계층적아이디={}",
                                request.getHqAccountNumber(), request.getHierarchicalId());

                try {
                        // 협력사 인증
                        Partner partner = partnerService.login(
                                        request.getHqAccountNumber(),
                                        request.getHierarchicalId(),
                                        request.getPassword());

                        // JWT 클레임 생성
                        AuthDto.JwtClaims claims = AuthDto.JwtClaims.builder()
                                        .accountNumber(partner.getFullAccountNumber())
                                        .companyName(partner.getCompanyName())
                                        .userType("PARTNER")
                                        .level(partner.getLevel())
                                        .treePath(partner.getTreePath())
                                        .headquartersId(partner.getHeadquarters().getId())
                                        .userId(partner.getId())
                                        .build();

                        // 토큰 생성
                        String accessToken = jwtUtil.generateAccessToken(claims);
                        String refreshToken = jwtUtil.generateRefreshToken(partner.getFullAccountNumber());

                        // JWT 쿠키 설정
                        setJwtCookie(response, accessToken);

                        // 응답 생성
                        AuthDto.TokenResponse tokenResponse = AuthDto.TokenResponse.of(
                                        accessToken,
                                        refreshToken,
                                        jwtUtil.getAccessTokenExpiration(),
                                        partner.getFullAccountNumber(),
                                        partner.getCompanyName(),
                                        "PARTNER",
                                        partner.getLevel());

                        log.info("협력사 로그인 성공: 계층적아이디={}", partner.getHierarchicalId());

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(tokenResponse, "로그인이 성공적으로 완료되었습니다."));
                } catch (Exception e) {
                        log.warn("협력사 로그인 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "LOGIN_FAILED"));
                }
        }

        /**
         * 협력사 정보 조회
         */
        @GetMapping("/{partnerId}")
        @Operation(summary = "협력사 정보 조회", description = "협력사 상세 정보를 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS') or (hasRole('PARTNER') and @securityUtil.getCurrentUserId() == #partnerId)")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<PartnerResponse>> getPartnerInfo(
                        @PathVariable Long partnerId) {

                log.info("협력사 정보 조회 요청: {}", partnerId);

                try {
                        Partner partner = partnerService.findById(partnerId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 협력사입니다: " + partnerId));

                        PartnerResponse response = PartnerResponse.from(partner);
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

        /**
         * 본사별 1차 협력사 목록 조회
         */
        @GetMapping("/headquarters/{headquartersId}/first-level")
        @Operation(summary = "1차 협력사 목록 조회", description = "본사의 1차 협력사 목록을 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS') and @securityUtil.getCurrentHeadquartersId() == #headquartersId")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerResponse>>> getFirstLevelPartners(
                        @PathVariable Long headquartersId) {

                log.info("1차 협력사 목록 조회 요청: 본사 ID {}", headquartersId);

                try {
                        List<Partner> partners = partnerService.findFirstLevelPartners(headquartersId);
                        List<PartnerResponse> response = partners.stream()
                                        .map(PartnerResponse::from)
                                        .toList();

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (Exception e) {
                        log.error("1차 협력사 목록 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 직접 하위 협력사 목록 조회
         */
        @GetMapping("/{parentId}/children")
        @Operation(summary = "직접 하위 협력사 목록 조회", description = "특정 협력사의 직접 하위 협력사 목록을 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS') or (hasRole('PARTNER') and @securityUtil.getCurrentUserId() == #parentId)")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerResponse>>> getDirectChildren(
                        @PathVariable Long parentId) {

                log.info("직접 하위 협력사 목록 조회 요청: 상위 협력사 ID {}", parentId);

                try {
                        List<Partner> partners = partnerService.findDirectChildren(parentId);
                        List<PartnerResponse> response = partners.stream()
                                        .map(PartnerResponse::from)
                                        .toList();

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (Exception e) {
                        log.error("직접 하위 협력사 목록 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 초기 비밀번호 변경
         */
        @PatchMapping("/{partnerId}/initial-password")
        @Operation(summary = "초기 비밀번호 변경", description = "협력사 첫 로그인 후 초기 비밀번호를 변경합니다")
        @PreAuthorize("hasRole('PARTNER') and @securityUtil.getCurrentUserId() == #partnerId")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<String>> changeInitialPassword(
                        @PathVariable Long partnerId,
                        @Valid @RequestBody PartnerInitialPasswordChangeRequest request) {

                log.info("초기 비밀번호 변경 요청: {}", partnerId);

                try {
                        // 비밀번호 확인 검증
                        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                                return ResponseEntity.badRequest()
                                                .body(AuthDto.ApiResponse.error("비밀번호 확인이 일치하지 않습니다.",
                                                                "PASSWORD_MISMATCH"));
                        }

                        partnerService.changeInitialPassword(partnerId, request.getNewPassword());
                        return ResponseEntity.ok(
                                        AuthDto.ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.", "초기 비밀번호 변경이 완료되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("초기 비밀번호 변경 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "PASSWORD_CHANGE_FAILED"));
                } catch (Exception e) {
                        log.error("초기 비밀번호 변경 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 비밀번호 미변경 협력사 목록 조회
         */
        @GetMapping("/headquarters/{headquartersId}/unchanged-password")
        @Operation(summary = "비밀번호 미변경 협력사 목록", description = "비밀번호를 아직 변경하지 않은 협력사 목록을 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS') and @securityUtil.getCurrentHeadquartersId() == #headquartersId")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerResponse>>> getUnchangedPasswordPartners(
                        @PathVariable Long headquartersId) {

                log.info("비밀번호 미변경 협력사 목록 조회 요청: 본사 ID {}", headquartersId);

                try {
                        List<Partner> partners = partnerService.findUnchangedPasswordPartners(headquartersId);
                        List<PartnerResponse> response = partners.stream()
                                        .map(PartnerResponse::from)
                                        .toList();

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response));
                } catch (Exception e) {
                        log.error("비밀번호 미변경 협력사 목록 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 로그아웃
         */
        @PostMapping("/logout")
        @Operation(summary = "협력사 로그아웃", description = "JWT 쿠키를 삭제하여 로그아웃 처리")
        public ResponseEntity<AuthDto.ApiResponse<String>> logout(HttpServletResponse response) {

                log.info("협력사 로그아웃 요청");

                // JWT 쿠키 삭제
                clearJwtCookie(response);

                return ResponseEntity.ok(AuthDto.ApiResponse.success("로그아웃 완료", "로그아웃이 성공적으로 완료되었습니다."));
        }

        /**
         * 이메일 중복 확인
         */
        @GetMapping("/check-email")
        @Operation(summary = "이메일 중복 확인", description = "협력사 생성 시 이메일 중복 여부 확인")
        public ResponseEntity<AuthDto.ApiResponse<Boolean>> checkEmail(@RequestParam String email) {

                boolean isDuplicate = partnerService.isEmailDuplicate(email);
                String message = isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";

                return ResponseEntity.ok(AuthDto.ApiResponse.success(!isDuplicate, message));
        }

        /**
         * JWT 토큰을 HttpOnly 쿠키에 설정
         */
        private void setJwtCookie(HttpServletResponse response, String token) {
                Cookie jwtCookie = new Cookie("jwt", token);
                jwtCookie.setHttpOnly(true); // XSS 방지
                jwtCookie.setSecure(true); // HTTPS에서만 전송
                jwtCookie.setPath("/"); // 모든 경로에서 사용
                jwtCookie.setMaxAge((int) (jwtUtil.getAccessTokenExpiration() / 1000)); // 토큰 만료시간과 동일
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
                response.addCookie(jwtCookie);
                log.debug("JWT 쿠키 삭제 완료");
        }
}