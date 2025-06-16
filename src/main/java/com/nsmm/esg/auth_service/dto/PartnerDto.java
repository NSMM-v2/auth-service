package com.nsmm.esg.auth_service.dto;

import com.nsmm.esg.auth_service.entity.Partner;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 협력사 관련 DTO 클래스들
 */
public class PartnerDto {

    /**
     * 협력사 계정 생성 요청 DTO (AWS IAM 방식)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "회사명은 필수입니다")
        @Size(max = 255, message = "회사명은 255자를 초과할 수 없습니다")
        private String companyName;
        
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
        
        @NotBlank(message = "담당자명은 필수입니다")
        @Size(max = 100, message = "담당자명은 100자를 초과할 수 없습니다")
        private String contactPerson;
        
        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
        private String phone;
        
        private String address;
        
        // 상위 협력사 ID (null이면 1차 협력사)
        private Long parentId;
    }

    /**
     * 협력사 계정 생성 응답 DTO (AWS IAM 방식)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResponse {
        
        private Long id;
        private String accountNumber;
        private String companyName;
        private String email;
        private String contactPerson;
        private String temporaryPassword;  // 임시 비밀번호
        private Integer level;
        private String treePath;
        private Partner.PartnerStatus status;
        private String message;
        
        /**
         * Entity를 CreateResponse DTO로 변환
         */
        public static CreateResponse from(Partner partner, String temporaryPassword) {
            return CreateResponse.builder()
                    .id(partner.getId())
                    .accountNumber(partner.getAccountNumber())
                    .companyName(partner.getCompanyName())
                    .email(partner.getEmail())
                    .contactPerson(partner.getContactPerson())
                    .temporaryPassword(temporaryPassword)
                    .level(partner.getLevel())
                    .treePath(partner.getTreePath())
                    .status(partner.getStatus())
                    .message("협력사 계정이 성공적으로 생성되었습니다. 임시 비밀번호로 로그인 후 비밀번호를 변경해주세요.")
                    .build();
        }
    }

    /**
     * 협력사 로그인 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        
        @NotBlank(message = "계정 번호는 필수입니다")
        private String accountNumber;
        
        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 협력사 정보 응답 DTO
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
        private String contactPerson;
        private String phone;
        private String address;
        private Integer level;
        private String treePath;
        private Partner.PartnerStatus status;
        private Boolean passwordChanged;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        // 상위 협력사 정보
        private Long parentId;
        private String parentAccountNumber;
        private String parentCompanyName;
        
        // 본사 정보
        private Long headquartersId;
        private String headquartersAccountNumber;
        
        /**
         * Entity를 DTO로 변환
         */
        public static Response from(Partner partner) {
            ResponseBuilder builder = Response.builder()
                    .id(partner.getId())
                    .accountNumber(partner.getAccountNumber())
                    .companyName(partner.getCompanyName())
                    .email(partner.getEmail())
                    .contactPerson(partner.getContactPerson())
                    .phone(partner.getPhone())
                    .address(partner.getAddress())
                    .level(partner.getLevel())
                    .treePath(partner.getTreePath())
                    .status(partner.getStatus())
                    .passwordChanged(partner.getPasswordChanged())
                    .createdAt(partner.getCreatedAt())
                    .updatedAt(partner.getUpdatedAt())
                    .headquartersId(partner.getHeadquarters().getId())
                    .headquartersAccountNumber(partner.getHeadquarters().generateAccountNumber());
            
            // 상위 협력사 정보 설정
            if (partner.getParent() != null) {
                builder.parentId(partner.getParent().getId())
                       .parentAccountNumber(partner.getParent().getAccountNumber())
                       .parentCompanyName(partner.getParent().getCompanyName());
            }
            
            return builder.build();
        }
    }

    /**
     * 협력사 목록 응답 DTO (트리 구조)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreeResponse {
        
        private Long id;
        private String accountNumber;
        private String companyName;
        private Integer level;
        private Partner.PartnerStatus status;
        private List<TreeResponse> children;
        
        /**
         * Entity를 TreeResponse DTO로 변환
         */
        public static TreeResponse from(Partner partner) {
            return TreeResponse.builder()
                    .id(partner.getId())
                    .accountNumber(partner.getAccountNumber())
                    .companyName(partner.getCompanyName())
                    .level(partner.getLevel())
                    .status(partner.getStatus())
                    .children(partner.getChildren().stream()
                            .map(TreeResponse::from)
                            .toList())
                    .build();
        }
    }

    /**
     * 협력사 정보 수정 요청 DTO
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
        private String contactPerson;
        
        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
        private String phone;
        
        private String address;
    }

    /**
     * 비밀번호 변경 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeRequest {
        
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        private String currentPassword;
        
        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
        private String newPassword;
        
        @NotBlank(message = "비밀번호 확인은 필수입니다")
        private String confirmPassword;
    }

    /**
     * 협력사 상태 변경 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusChangeRequest {
        
        @NotNull(message = "상태는 필수입니다")
        private Partner.PartnerStatus status;
        
        private String reason;  // 상태 변경 사유
    }
} 