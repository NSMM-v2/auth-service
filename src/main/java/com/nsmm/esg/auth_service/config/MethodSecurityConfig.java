package com.nsmm.esg.auth_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 메서드 레벨 보안 설정
 * @PreAuthorize, @PostAuthorize 등의 어노테이션 사용 가능
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MethodSecurityConfig {
} 