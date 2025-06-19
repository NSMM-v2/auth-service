package com.nsmm.esg.auth_service.controller;

import com.nsmm.esg.auth_service.dto.ApiResponse;
import com.nsmm.esg.auth_service.dto.JwtClaims;
import com.nsmm.esg.auth_service.dto.TokenResponse;
import com.nsmm.esg.auth_service.dto.headquarters.HeadquartersLoginRequest;
import com.nsmm.esg.auth_service.dto.headquarters.HeadquartersSignupRequest;
import com.nsmm.esg.auth_service.dto.headquarters.HeadquartersSignupResponse;
import com.nsmm.esg.auth_service.dto.headquarters.HeadquartersResponse;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.service.HeadquartersService;
import com.nsmm.esg.auth_service.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 본사 관리 컨트롤러
 * 
 * 주요 기능:
 * - 본사 회원가입 (동적 계정번호 생성, UUID 자동 생성)
 * - 이메일 기반 로그인
 * - UUID 기반 조회
 * - JWT 쿠키 인증
 * - 본사 정보 관리
 */
@RestController
@RequestMapping("/api/v1/headquarters")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "본사 관리", description = "본사 회원가입, 로그인, 정보 관리 API")
public class HeadquartersController {

        private final HeadquartersService headquartersService;
        private final JwtUtil jwtUtil;

        /**
         * 본사 회원가입
         * 계정번호 자동 생성 (HQ + YYYYMMDD + 순번), UUID 자동 생성, 이메일 중복 검사
         */
        @PostMapping("/register")
        @Operation(summary = "본사 회원가입", description = "계정번호 및 UUID 자동 생성을 통한 본사 회원가입")
        public ResponseEntity<ApiResponse<HeadquartersSignupResponse>> register(
                        @Valid @RequestBody HeadquartersSignupRequest request) {

                log.info("본사 회원가입 요청: 이메일={}", request.getEmail());

                try {
                        Headquarters headquarters = headquartersService.register(request);
                        HeadquartersSignupResponse response = HeadquartersSignupResponse.from(headquarters);

                        return ResponseEntity.ok(ApiResponse.success(response, "본사 회원가입이 성공적으로 완료되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("본사 회원가입 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error(e.getMessage(), "REGISTRATION_FAILED"));
                } catch (IllegalStateException e) {
                        log.warn("본사 회원가입 실패 (시스템 제한): {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error(e.getMessage(), "SYSTEM_LIMIT_EXCEEDED"));
                }
        }

        /**
         * 본사 로그인
         * 이메일 + 비밀번호 → JWT 쿠키 설정
         */
        @PostMapping("/login")
        @Operation(summary = "본사 로그인", description = "이메일 기반 로그인 후 JWT 쿠키 설정")
        public ResponseEntity<ApiResponse<TokenResponse>> login(
                        @Valid @RequestBody HeadquartersLoginRequest request,
                        HttpServletResponse response) {

                log.info("본사 로그인 요청: 이메일={}", request.getEmail());

                try {
                        // 본사 인증
                        Headquarters headquarters = headquartersService.login(request);

                        // JWT 클레임 생성
                        JwtClaims claims = JwtClaims.builder()
                                        .accountNumber(headquarters.getHqAccountNumber())
                                        .companyName(headquarters.getCompanyName())
                                        .userType("HEADQUARTERS")
                                        .level(null) // 본사는 레벨 없음
                                        .treePath(null) // 본사는 트리 경로 없음
                                        .headquartersId(headquarters.getHeadquartersId())
                                        .partnerId(null) // 본사는 협력사 ID 없음
                                        .build();

                        // 토큰 생성
                        String accessToken = jwtUtil.generateAccessToken(claims);
                        String refreshToken = jwtUtil.generateRefreshToken(headquarters.getHqAccountNumber());

                        // JWT 쿠키 설정
                        setJwtCookie(response, accessToken);

                        // 응답 생성
                        TokenResponse tokenResponse = TokenResponse.of(
                                        accessToken,
                                        refreshToken,
                                        jwtUtil.getAccessTokenExpiration(),
                                        headquarters.getHqAccountNumber(),
                                        headquarters.getCompanyName(),
                                        "HEADQUARTERS",
                                        null);

                        log.info("본사 로그인 성공: 계정번호={}", headquarters.getHqAccountNumber());

                        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "로그인이 성공적으로 완료되었습니다."));
                } catch (Exception e) {
                        log.warn("본사 로그인 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error(e.getMessage(), "LOGIN_FAILED"));
                }
        }

        /**
         * 본사 로그아웃
         * JWT 쿠키 삭제
         */
        @PostMapping("/logout")
        @Operation(summary = "본사 로그아웃", description = "JWT 쿠키 삭제")
        public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {

                log.info("본사 로그아웃 요청");

                // JWT 쿠키 삭제
                clearJwtCookie(response);

                return ResponseEntity.ok(ApiResponse.success("로그아웃 완료", "로그아웃이 성공적으로 완료되었습니다."));
        }

        /**
         * UUID로 본사 정보 조회
         */
        @GetMapping("/by-uuid/{uuid}")
        @Operation(summary = "UUID로 본사 정보 조회", description = "UUID를 이용해 본사 정보를 조회합니다")
        public ResponseEntity<ApiResponse<HeadquartersResponse>> getHeadquartersByUuid(
                        @PathVariable String uuid) {

                log.info("UUID로 본사 정보 조회 요청: {}", uuid);

                try {
                        Headquarters headquarters = headquartersService.findByUuid(uuid)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 본사입니다: " + uuid));

                        HeadquartersResponse response = HeadquartersResponse.from(headquarters);

                        return ResponseEntity.ok(ApiResponse.success(response, "본사 정보가 조회되었습니다."));
                } catch (IllegalArgumentException e) {
                        log.warn("UUID로 본사 정보 조회 실패: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error(e.getMessage(), "HEADQUARTERS_NOT_FOUND"));
                } catch (Exception e) {
                        log.error("UUID로 본사 정보 조회 중 오류 발생", e);
                        return ResponseEntity.status(500)
                                        .body(ApiResponse.error("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
                }
        }

        /**
         * 이메일 중복 확인
         */
        @GetMapping("/check-email")
        @Operation(summary = "이메일 중복 확인", description = "회원가입 시 이메일 중복 여부 확인")
        public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {

                boolean isDuplicate = headquartersService.isEmailDuplicate(email);
                String message = isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";

                return ResponseEntity.ok(ApiResponse.success(!isDuplicate, message));
        }

        /**
         * UUID 중복 확인
         */
        @GetMapping("/check-uuid")
        @Operation(summary = "UUID 중복 확인", description = "본사 생성 시 UUID 중복 여부 확인")
        public ResponseEntity<ApiResponse<Boolean>> checkUuid(@RequestParam String uuid) {

                try {
                        boolean isDuplicate = headquartersService.isUuidDuplicate(uuid);
                        String message = isDuplicate ? "이미 사용 중인 UUID입니다." : "사용 가능한 UUID입니다.";

                        return ResponseEntity.ok(ApiResponse.success(!isDuplicate, message));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("잘못된 UUID 형식입니다.", "INVALID_UUID"));
                }
        }

        /**
         * 다음 생성될 본사 계정번호 미리 확인
         */
        @GetMapping("/next-account-number")
        @Operation(summary = "다음 계정번호 확인", description = "다음에 생성될 본사 계정번호를 미리 확인")
        public ResponseEntity<ApiResponse<String>> getNextAccountNumber() {

                String nextAccountNumber = headquartersService.getNextAccountNumber();

                if (nextAccountNumber != null) {
                        return ResponseEntity.ok(ApiResponse.success(nextAccountNumber,
                                        "다음 생성될 계정번호입니다."));
                } else {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("계정번호 생성에 실패했습니다.", "GENERATION_FAILED"));
                }
        }

        /**
         * 본사 계정번호 유효성 검증
         */
        @GetMapping("/validate-account-number")
        @Operation(summary = "계정번호 유효성 검증", description = "본사 계정번호 형식 유효성을 검증")
        public ResponseEntity<ApiResponse<Boolean>> validateAccountNumber(@RequestParam String accountNumber) {

                boolean isValid = headquartersService.isValidAccountNumber(accountNumber);
                String message = isValid ? "올바른 계정번호 형식입니다." : "잘못된 계정번호 형식입니다.";

                return ResponseEntity.ok(ApiResponse.success(isValid, message));
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