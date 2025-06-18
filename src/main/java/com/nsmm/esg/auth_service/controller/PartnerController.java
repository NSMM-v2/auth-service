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
import com.nsmm.esg.auth_service.util.SecurityUtil;
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
 * - UUID 기반 협력사 생성 (프론트엔드 요구사항 반영)
 * - 계층적 로그인 (본사계정번호 + 계층적아이디 + 비밀번호)
 * - 권한별 접근 제어 (본사: 모든 데이터, 협력사: 본인+직속하위 1단계)
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
        private final SecurityUtil securityUtil;

        /**
         * UUID 기반 협력사 생성 (DART API 기반)
         * 요청값: 다른 서비스에서 DART API를 통해 제공받은 회사 정보
         * - UUID: DART API 회사 고유 식별자
         * - contactPerson: DART API 대표자명
         * - companyName: DART API 회사명
         * - address: DART API 회사 주소
         * - parentUuid: 상위 협력사 UUID (1차 협력사면 null)
         */
        @PostMapping("/create-by-uuid")
        @Operation(summary = "협력사 생성 (DART API 기반)", description = "DART API에서 제공받은 회사 정보로 협력사를 생성합니다")
        @PreAuthorize("hasRole('HEADQUARTERS') or hasRole('PARTNER')")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<PartnerCreateResponse>> createPartnerByUuid(
                        @Valid @RequestBody PartnerCreateRequest request) {

                log.info("DART API 기반 협력사 생성 요청: UUID={}, 회사명={}, 상위UUID={}",
                                request.getUuid(), request.getCompanyName(), request.getParentUuid());

                try {
                        // JWT에서 현재 사용자 정보 추출
                        String userType = securityUtil.getCurrentUserType();

                        Partner partner;
                        if ("HEADQUARTERS".equals(userType)) {
                                // 본사: 모든 협력사 생성 가능
                                Long headquartersId = securityUtil.getCurrentHeadquartersId();

                                // parentUuid가 null인 경우 1차 협력사 생성
                                if (request.getParentUuid() == null) {
                                        // 본사의 UUID를 parentUuid로 설정
                                        Headquarters headquarters = headquartersService.findById(headquartersId)
                                                        .orElseThrow(() -> new IllegalArgumentException(
                                                                        "존재하지 않는 본사입니다: " + headquartersId));

                                        // 본사 UUID를 상위 UUID로 설정한 새로운 요청 생성
                                        PartnerCreateRequest modifiedRequest = PartnerCreateRequest.builder()
                                                        .uuid(request.getUuid())
                                                        .contactPerson(request.getContactPerson())
                                                        .companyName(request.getCompanyName())
                                                        .address(request.getAddress())
                                                        .parentUuid(headquarters.getUuid())
                                                        .build();

                                        partner = partnerService.createPartnerByUuid(headquartersId, modifiedRequest);
                                } else {
                                        partner = partnerService.createPartnerByUuid(headquartersId, request);
                                }
                        } else {
                                // 협력사: 자신의 하위만 생성 가능 (권한 검증은 서비스에서 처리)
                                Long headquartersId = securityUtil.getCurrentHeadquartersId();
                                partner = partnerService.createPartnerByUuid(headquartersId, request);
                        }

                        PartnerCreateResponse response = PartnerCreateResponse.from(partner);

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(AuthDto.ApiResponse.success(response, "DART API 기반 협력사가 성공적으로 생성되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("DART API 기반 협력사 생성 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "CREATE_FAILED"));
                } catch (Exception e) {
                        log.error("DART API 기반 협력사 생성 중 오류 발생", e);
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
                                        .headquartersId(partner.getHeadquarters().getHeadquartersId())
                                        .partnerId(partner.getPartnerId())
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
        @PreAuthorize("hasRole('HEADQUARTERS') or (hasRole('PARTNER') and @securityUtil.getCurrentEntityId() == #partnerId)")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<PartnerResponse>> getPartnerInfo(
                        @PathVariable Long partnerId) {

                log.info("협력사 정보 조회 요청: {}", partnerId);

                try {
                        Partner partner = partnerService.findById(partnerId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 협력사입니다: " + partnerId));

                        PartnerResponse response = PartnerResponse.from(partner);

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response, "협력사 정보가 조회되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("협력사 정보 조회 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "PARTNER_NOT_FOUND"));
                } catch (Exception e) {
                        log.error("협력사 정보 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * UUID로 협력사 정보 조회
         */
        @GetMapping("/by-uuid/{uuid}")
        @Operation(summary = "UUID로 협력사 정보 조회", description = "UUID를 이용해 협력사 정보를 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS') or hasRole('PARTNER')")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<PartnerResponse>> getPartnerByUuid(
                        @PathVariable String uuid) {

                log.info("UUID로 협력사 정보 조회 요청: {}", uuid);

                try {
                        Partner partner = partnerService.findByUuid(uuid)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 협력사입니다: " + uuid));

                        PartnerResponse response = PartnerResponse.from(partner);

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(response, "협력사 정보가 조회되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("UUID로 협력사 정보 조회 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "PARTNER_NOT_FOUND"));
                } catch (Exception e) {
                        log.error("UUID로 협력사 정보 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 접근 가능한 협력사 목록 조회 (권한별 제어)
         * 본사: 모든 협력사, 협력사: 본인 + 직속하위 1단계
         */
        @GetMapping("/accessible")
        @Operation(summary = "접근 가능한 협력사 목록 조회", description = "권한에 따라 접근 가능한 협력사 목록을 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS') or hasRole('PARTNER')")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerResponse>>> getAccessiblePartners() {

                log.info("접근 가능한 협력사 목록 조회 요청");

                try {
                        String userType = securityUtil.getCurrentUserType();
                        List<Partner> partners;

                        if ("HEADQUARTERS".equals(userType)) {
                                // 본사: 모든 협력사 접근 가능
                                Long headquartersId = securityUtil.getCurrentHeadquartersId();
                                partners = partnerService.findAccessiblePartners("HEADQUARTERS", headquartersId, null,
                                                null);
                        } else {
                                // 협력사: 본인 + 직속하위 1단계만 접근 가능
                                Long currentPartnerId = securityUtil.getCurrentEntityId();
                                Partner currentPartner = partnerService.findById(currentPartnerId)
                                                .orElseThrow(() -> new IllegalArgumentException(
                                                                "존재하지 않는 협력사입니다: " + currentPartnerId));
                                partners = partnerService.findAccessiblePartners("PARTNER", currentPartnerId,
                                                currentPartner.getTreePath(), currentPartner.getLevel());
                        }

                        List<PartnerResponse> responses = partners.stream()
                                        .map(PartnerResponse::from)
                                        .toList();

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(responses,
                                        "접근 가능한 협력사 목록이 조회되었습니다. (총 " + responses.size() + "개)"));
                } catch (Exception e) {
                        log.error("접근 가능한 협력사 목록 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 1차 협력사 목록 조회 (본사 전용)
         */
        @GetMapping("/first-level")
        @Operation(summary = "1차 협력사 목록 조회", description = "본사의 1차 협력사 목록을 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS')")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerResponse>>> getFirstLevelPartners() {

                log.info("1차 협력사 목록 조회 요청");

                try {
                        // JWT에서 본사 ID 추출
                        Long headquartersId = securityUtil.getCurrentHeadquartersId();

                        // 1차 협력사 목록 조회
                        List<Partner> partners = partnerService.findFirstLevelPartners(headquartersId);

                        List<PartnerResponse> responses = partners.stream()
                                        .map(PartnerResponse::from)
                                        .toList();

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(responses,
                                        "1차 협력사 목록이 조회되었습니다. (총 " + responses.size() + "개)"));
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
        @PreAuthorize("hasRole('HEADQUARTERS') or (hasRole('PARTNER') and @securityUtil.getCurrentEntityId() == #parentId)")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerResponse>>> getDirectChildren(
                        @PathVariable Long parentId) {

                log.info("직접 하위 협력사 목록 조회 요청: 상위ID={}", parentId);

                try {
                        // 상위 협력사 조회
                        Partner parent = partnerService.findById(parentId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 협력사입니다: " + parentId));

                        // 직접 하위 협력사 목록 조회
                        List<Partner> children = partnerService.findDirectChildren(parent.getPartnerId());

                        List<PartnerResponse> responses = children.stream()
                                        .map(PartnerResponse::from)
                                        .toList();

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(responses,
                                        "직접 하위 협력사 목록이 조회되었습니다. (총 " + responses.size() + "개)"));
                } catch (IllegalArgumentException e) {
                        log.warn("직접 하위 협력사 목록 조회 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error(e.getMessage(), "PARENT_NOT_FOUND"));
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
        @PreAuthorize("hasRole('PARTNER') and @securityUtil.getCurrentEntityId() == #partnerId")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<String>> changeInitialPassword(
                        @PathVariable Long partnerId,
                        @Valid @RequestBody PartnerInitialPasswordChangeRequest request) {

                log.info("초기 비밀번호 변경 요청: 협력사ID={}", partnerId);

                try {
                        // 협력사 조회
                        Partner partner = partnerService.findById(partnerId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 협력사입니다: " + partnerId));

                        // 초기 비밀번호 변경 (현재 비밀번호 검증은 생략, DTO에 currentPassword 필드 없음)
                        partnerService.changeInitialPassword(partner.getPartnerId(), request.getNewPassword());

                        return ResponseEntity.ok(AuthDto.ApiResponse.success("비밀번호 변경 완료",
                                        "초기 비밀번호가 성공적으로 변경되었습니다."));
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
         * 비밀번호 미변경 협력사 목록 (본사 전용)
         */
        @GetMapping("/unchanged-password")
        @Operation(summary = "비밀번호 미변경 협력사 목록", description = "비밀번호를 아직 변경하지 않은 협력사 목록을 조회합니다")
        @PreAuthorize("hasRole('HEADQUARTERS')")
        @SecurityRequirement(name = "JWT")
        public ResponseEntity<AuthDto.ApiResponse<List<PartnerResponse>>> getUnchangedPasswordPartners() {

                log.info("비밀번호 미변경 협력사 목록 조회 요청");

                try {
                        // JWT에서 본사 ID 추출
                        Long headquartersId = securityUtil.getCurrentHeadquartersId();

                        // 비밀번호 미변경 협력사 목록 조회
                        List<Partner> partners = partnerService.findUnchangedPasswordPartners(headquartersId);

                        List<PartnerResponse> responses = partners.stream()
                                        .map(PartnerResponse::from)
                                        .toList();

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(responses,
                                        "비밀번호 미변경 협력사 목록이 조회되었습니다. (총 " + responses.size() + "개)"));
                } catch (Exception e) {
                        log.error("비밀번호 미변경 협력사 목록 조회 중 오류 발생", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(AuthDto.ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 협력사 로그아웃
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
         * UUID 중복 확인
         */
        @GetMapping("/check-uuid")
        @Operation(summary = "UUID 중복 확인", description = "협력사 생성 시 UUID 중복 여부 확인")
        public ResponseEntity<AuthDto.ApiResponse<Boolean>> checkUuid(@RequestParam String uuid) {

                try {
                        boolean isDuplicate = partnerService.isUuidDuplicate(uuid);
                        String message = isDuplicate ? "이미 사용 중인 UUID입니다." : "사용 가능한 UUID입니다.";

                        return ResponseEntity.ok(AuthDto.ApiResponse.success(!isDuplicate, message));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                        .body(AuthDto.ApiResponse.error("잘못된 UUID 형식입니다.", "INVALID_UUID"));
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