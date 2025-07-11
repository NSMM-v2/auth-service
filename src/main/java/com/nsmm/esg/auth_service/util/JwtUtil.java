package com.nsmm.esg.auth_service.util;

import com.nsmm.esg.auth_service.dto.JwtClaims;
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

    public JwtUtil(
            @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidationPurposeOnly123456789}") String secret,
            @Value("${jwt.expiration:900000}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpiration) { // n 하나 제거
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration; // 이제 매치됨
    }

    /**
     * Access Token 생성
     */
    public String generateAccessToken(JwtClaims claims) {
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
     * 토큰에서 모든 클레임 정보 추출
     */
    public JwtClaims getAllClaimsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);

        return JwtClaims.builder()
                .accountNumber(claims.getSubject())
                .companyName(claims.get("companyName", String.class))
                .userType(claims.get("userType", String.class))
                .level(claims.get("level", Integer.class))
                .treePath(claims.get("treePath", String.class))
                .headquartersId(claims.get("headquartersId", Long.class))
                .partnerId(claims.get("partnerId", Long.class))
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
    private Map<String, Object> createClaimsMap(JwtClaims claims) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("companyName", claims.getCompanyName());
        claimsMap.put("userType", claims.getUserType());
        claimsMap.put("headquartersId", claims.getHeadquartersId());

        // 협력사인 경우에만 partnerId 추가
        if ("PARTNER".equals(claims.getUserType()) && claims.getPartnerId() != null) {
            claimsMap.put("partnerId", claims.getPartnerId());
            claimsMap.put("level", claims.getLevel());
            claimsMap.put("treePath", claims.getTreePath());
        }

        return claimsMap;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }


}