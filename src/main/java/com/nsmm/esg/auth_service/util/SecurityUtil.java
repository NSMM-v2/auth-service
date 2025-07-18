package com.nsmm.esg.auth_service.util;

import com.nsmm.esg.auth_service.dto.JwtClaims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Spring Security 인증 정보 유틸리티
 */
@Service("securityUtil")
public class SecurityUtil {

    /**
     * 현재 인증된 사용자의 JWT Claims 반환
     */
    public JwtClaims getCurrentUserClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof JwtClaims) {
            return (JwtClaims) authentication.getPrincipal();
        }

        throw new IllegalStateException("인증되지 않은 사용자입니다.");
    }

    /**
     * 현재 사용자의 계정 번호 반환
     */
    public String getCurrentAccountNumber() {
        return getCurrentUserClaims().getAccountNumber();
    }

    /**
     * 현재 사용자의 타입 반환 (HEADQUARTERS 또는 PARTNER)
     */
    public String getCurrentUserType() {
        return getCurrentUserClaims().getUserType();
    }

    /**
     * 현재 사용자의 회사명 반환
     */
    public String getCurrentCompanyName() {
        return getCurrentUserClaims().getCompanyName();
    }

    /**
     * 현재 사용자의 본사 ID 반환
     */
    public Long getCurrentHeadquartersId() {
        return getCurrentUserClaims().getHeadquartersId();
    }

    /**
     * 현재 사용자의 협력사 ID 반환 (협력사인 경우)
     */
    public Long getCurrentPartnerId() {
        JwtClaims claims = getCurrentUserClaims();
        if (isPartner()) {
            return claims.getPartnerId();
        }
        throw new IllegalStateException("본사 사용자는 협력사 ID가 없습니다.");
    }

    /**
     * 현재 사용자의 엔티티 ID 반환 (본사면 headquartersId, 협력사면 partnerId)
     */
    public Long getCurrentEntityId() {
        JwtClaims claims = getCurrentUserClaims();
        if (isHeadquarters()) {
            return claims.getHeadquartersId();
        } else if (isPartner()) {
            return claims.getPartnerId();
        }
        throw new IllegalStateException("알 수 없는 사용자 타입입니다.");
    }

    /**
     * 현재 사용자가 본사인지 확인
     */
    public boolean isHeadquarters() {
        return "HEADQUARTERS".equals(getCurrentUserType());
    }

    /**
     * 현재 사용자가 협력사인지 확인
     */
    public boolean isPartner() {
        return "PARTNER".equals(getCurrentUserType());
    }

    /**
     * 현재 사용자의 레벨 반환 (협력사인 경우)
     */
    public Integer getCurrentLevel() {
        JwtClaims claims = getCurrentUserClaims();
        if (isPartner()) {
            return claims.getLevel();
        }
        return null;
    }

    /**
     * 현재 사용자의 트리 경로 반환 (협력사인 경우)
     */
    public String getCurrentTreePath() {
        JwtClaims claims = getCurrentUserClaims();
        if (isPartner()) {
            return claims.getTreePath();
        }
        return null;
    }

    /**
     * 인증된 사용자인지 확인
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof JwtClaims;
    }
}