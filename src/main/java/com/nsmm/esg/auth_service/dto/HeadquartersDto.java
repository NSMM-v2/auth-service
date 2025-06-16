package com.nsmm.esg.auth_service.dto;

import com.nsmm.esg.auth_service.entity.Headquarters;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 본사 관련 DTO 클래스들
 */
public class HeadquartersDto {

    /**
     * 본사 회원가입 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {
        
        @NotBlank(message = "회사명은 필수입니다")
        @Size(max = 255, message = "회사명은 255자를 초과할 수 없습니다")
        private String companyName;
        
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
        
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
        private String password;
        
        @NotBlank(message = "담당자명은 필수입니다")
        @Size(max = 100, message = "담당자명은 100자를 초과할 수 없습니다")
        private String name;
        
        @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다")
        private String department;
        
        @Size(max = 50, message = "직급은 50자를 초과할 수 없습니다")
        private String position;
        
        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
        private String phone;
        
        private String address;
    }

    /**
     * 본사 로그인 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
        
        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 본사 정보 응답 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        private Long id;
        private String accountNumber;
        private String companyName;
        private String email;
        private String name;
        private String department;
        private String position;
        private String phone;
        private String address;
        private Headquarters.CompanyStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        /**
         * Entity를 DTO로 변환
         */
        public static Response from(Headquarters headquarters) {
            return Response.builder()
                    .id(headquarters.getId())
                    .accountNumber(headquarters.generateAccountNumber())
                    .companyName(headquarters.getCompanyName())
                    .email(headquarters.getEmail())
                    .name(headquarters.getName())
                    .department(headquarters.getDepartment())
                    .position(headquarters.getPosition())
                    .phone(headquarters.getPhone())
                    .address(headquarters.getAddress())
                    .status(headquarters.getStatus())
                    .createdAt(headquarters.getCreatedAt())
                    .updatedAt(headquarters.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 본사 정보 수정 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 255, message = "회사명은 255자를 초과할 수 없습니다")
        private String companyName;
        
        @Size(max = 100, message = "담당자명은 100자를 초과할 수 없습니다")
        private String name;
        
        @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다")
        private String department;
        
        @Size(max = 50, message = "직급은 50자를 초과할 수 없습니다")
        private String position;
        
        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
        private String phone;
        
        private String address;
    }
} 