package com.nsmm.esg.auth_service.config;

import com.nsmm.esg.auth_service.dto.JwtClaims;
import com.nsmm.esg.auth_service.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터 - 쿠키 기반 JWT 토큰 검증만 지원
 * 모든 요청에서 쿠키의 JWT 토큰을 검증하고 인증 정보를 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 쿠키에서 JWT 토큰 추출
            String jwt = getJwtFromCookie(request);

            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                // JWT에서 사용자 정보 추출
                JwtClaims claims = jwtUtil.getAllClaimsFromToken(jwt);

                // 권한 설정
                String role = "ROLE_" + claims.getUserType(); // ROLE_HEADQUARTERS 또는 ROLE_PARTNER
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

                // 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(claims,
                        null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 쿠키 인증 성공: {} ({})", claims.getAccountNumber(), claims.getUserType());
            }
        } catch (Exception e) {
            log.error("JWT 쿠키 인증 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에서 JWT 토큰 추출
     */
    private String getJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 필터를 적용하지 않을 경로 설정
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 공개 API는 JWT 검증 제외
        return path.startsWith("/api/v1/headquarters/register") ||
                path.startsWith("/api/v1/headquarters/login") ||
                path.startsWith("/api/v1/headquarters/logout") ||
                path.startsWith("/api/v1/headquarters/check-email") ||
                path.startsWith("/api/v1/headquarters/check-uuid") ||
                path.startsWith("/api/v1/headquarters/by-uuid/") ||
                path.startsWith("/api/v1/headquarters/next-account-number") ||
                path.startsWith("/api/v1/headquarters/validate-account-number") ||
                path.startsWith("/api/v1/partners/login") ||
                path.startsWith("/api/v1/partners/logout") ||
                path.startsWith("/api/v1/partners/check-email") ||
                path.startsWith("/api/v1/partners/check-uuid") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/h2-console/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api-docs") ||
                path.equals("/error");
    }
}