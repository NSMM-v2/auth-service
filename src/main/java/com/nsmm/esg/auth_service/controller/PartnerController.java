package com.nsmm.esg.auth_service.controller;

import com.nsmm.esg.auth_service.dto.AuthDto;
import com.nsmm.esg.auth_service.dto.PartnerDto;
import com.nsmm.esg.auth_service.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 협력사 컨트롤러 (AWS IAM 방식)
 * 계층적 협력사 계정 생성, 로그인, 관리 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    /**
     * 협력사 계정 생성 (AWS IAM 방식)
     * 본사 또는 상위 협력사에서 하위 협력사 계정을 생성
     */
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

    /**
     * 협력사 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<AuthDto.ApiResponse<AuthDto.TokenResponse>> login(
            @Valid @RequestBody PartnerDto.LoginRequest request) {
        
        log.info("협력사 로그인 요청: {}", request.getAccountNumber());
        
        try {
            AuthDto.TokenResponse response = partnerService.login(request);
            return ResponseEntity.ok(AuthDto.ApiResponse.success(response, "로그인이 완료되었습니다."));
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

    /**
     * 협력사 정보 조회
     */
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

    /**
     * 본사의 1차 협력사 목록 조회
     */
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

    /**
     * 상위 협력사의 직접 하위 협력사 목록 조회
     */
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

    /**
     * 트리 구조 조회 (특정 협력사의 모든 하위 협력사)
     */
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

    /**
     * 협력사 정보 수정
     */
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

    /**
     * 비밀번호 변경
     */
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

    /**
     * 협력사 상태 변경
     */
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

    /**
     * 비밀번호 변경이 필요한 협력사 목록 조회 (관리자용)
     */
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
} 