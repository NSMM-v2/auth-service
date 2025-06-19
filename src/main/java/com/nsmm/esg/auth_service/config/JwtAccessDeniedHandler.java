package com.nsmm.esg.auth_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmm.esg.auth_service.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 접근 거부 시 처리하는 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper; // Spring에서 관리하는 ObjectMapper 주입

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.error("접근 권한이 없는 요청: {} {}", request.getMethod(), request.getRequestURI());

        // 응답 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // 에러 응답 생성
        ApiResponse<Object> errorResponse = ApiResponse.error(
                "접근 권한이 없습니다. 관리자에게 문의하세요.",
                "ACCESS_DENIED");

        // JSON 응답 작성
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}