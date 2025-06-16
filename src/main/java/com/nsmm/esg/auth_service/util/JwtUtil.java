package com.nsmm.esg.auth_service.util;

import com.nsmm.esg.auth_service.dto.AuthDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long accessTokenExpiration,
                   @Value("${jwt.refresh-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Access Token 생성
     */
    public String generateAccessToken(AuthDto.JwtClaims claims) {
        Map<String, Object> claimsMap = createClaimsMap(claims);
        
        return Jwts.builder()
                .setClaims(claimsMap)
                .setSubject(claims.getAccountNumber())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(String accountNumber) {
        return Jwts.builder()
                .setSubject(accountNumber)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 토큰에서 계정 번호 추출
     */
    public String getAccountNumberFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 토큰에서 사용자 타입 추출
     */
    public String getUserTypeFromToken(String token) {
        return getClaimsFromToken(token).get("userType", String.class);
    }

    /**
     * 토큰에서 회사명 추출
     */
    public String getCompanyNameFromToken(String token) {
        return getClaimsFromToken(token).get("companyName", String.class);
    }

    /**
     * 토큰에서 레벨 추출 (협력사인 경우)
     */
    public Integer getLevelFromToken(String token) {
        return getClaimsFromToken(token).get("level", Integer.class);
    }

    /**
     * 토큰에서 트리 경로 추출 (협력사인 경우)
     */
    public String getTreePathFromToken(String token) {
        return getClaimsFromToken(token).get("treePath", String.class);
    }

    /**
     * 토큰에서 본사 ID 추출
     */
    public Long getHeadquartersIdFromToken(String token) {
        return getClaimsFromToken(token).get("headquartersId", Long.class);
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    /**
     * 토큰에서 만료 시간 추출
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰 만료 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 토큰에서 모든 클레임 정보 추출
     */
    public AuthDto.JwtClaims getAllClaimsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        
        return AuthDto.JwtClaims.builder()
                .accountNumber(claims.getSubject())
                .companyName(claims.get("companyName", String.class))
                .userType(claims.get("userType", String.class))
                .level(claims.get("level", Integer.class))
                .treePath(claims.get("treePath", String.class))
                .headquartersId(claims.get("headquartersId", Long.class))
                .userId(claims.get("userId", Long.class))
                .build();
    }

    /**
     * 토큰에서 Claims 추출
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Claims Map 생성
     */
    private Map<String, Object> createClaimsMap(AuthDto.JwtClaims claims) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("companyName", claims.getCompanyName());
        claimsMap.put("userType", claims.getUserType());
        claimsMap.put("headquartersId", claims.getHeadquartersId());
        claimsMap.put("userId", claims.getUserId());
        
        // 협력사인 경우에만 추가
        if ("PARTNER".equals(claims.getUserType())) {
            claimsMap.put("level", claims.getLevel());
            claimsMap.put("treePath", claims.getTreePath());
        }
        
        return claimsMap;
    }

    /**
     * Access Token 만료 시간 반환 (밀리초)
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Refresh Token 만료 시간 반환 (밀리초)
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

} 