package com.nsmm.esg.auth_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3 설정 - 쿠키 기반 JWT 인증만 지원
 * API 문서화 및 테스트 인터페이스 제공
 */
@Configuration
public class SwaggerConfig {

        /**
         * OpenAPI 3 설정 - 쿠키 기반 인증만 지원
         */
        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(apiInfo())
                                .servers(List.of(
                                                new Server().url("http://localhost:8081").description("개발 서버"),
                                                new Server().url("https://api.esg-project.com").description("운영 서버")))
                                .components(new Components()
                                                .addSecuritySchemes("JWT", cookieSecurityScheme()))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList("JWT"));
        }

        /**
         * API 기본 정보
         */
        private Info apiInfo() {
                return new Info()
                                .title("ESG Project - Auth Service API")
                                .description("ESG 프로젝트 인증 서비스 API 문서 (쿠키 기반 JWT 인증)")
                                .version("1.0.0")
                                .contact(new Contact()
                                                .name("ESG Project Team")
                                                .email("dev@esg-project.com")
                                                .url("https://github.com/esg-project"))
                                .license(new License()
                                                .name("MIT License")
                                                .url("https://opensource.org/licenses/MIT"));
        }

        /**
         * 쿠키 기반 JWT 보안 스키마 설정
         */
        private SecurityScheme cookieSecurityScheme() {
                return new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("jwt")
                                .description("JWT 토큰을 HttpOnly 쿠키로 전송 (로그인 후 자동 설정)");
        }
}