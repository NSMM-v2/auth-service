package com.nsmm.esg.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Auth Service 메인 애플리케이션
 * AWS IAM 방식의 계층적 계정 관리 시스템
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
