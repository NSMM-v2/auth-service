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
     * AWS 스타일 인증 계정 생성 요청 DTO (다른 서비스에서 호출)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthAccountCreateRequest {

        @NotNull(message = "파트너 ID는 필수입니다")
        private Long partnerId;

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

        @NotNull(message = "본사 ID는 필수입니다")
        private Long headquartersId;

        // 상위 협력사 ID (선택사항 - 1차 협력사면 null)
        private Long parentId;
    }

    /**
     * AWS 스타일 인증 계정 생성 응답 DTO (물리적 전달용)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthAccountCreateResponse {

        private Long authId; // Auth Service의 내부 ID
        private Long partnerId; // 원본 파트너 ID (다른 서비스)
        private String accountNumber; // AWS 스타일 12자리 숫자
        private String loginId; // 로그인용 ID (partner_123456789012)
        private String temporaryPassword; // 임시 비밀번호
        private String companyName;
        private String contactPerson;
        private LocalDateTime createdAt;
        private String message;

        /**
         * 물리적 전달용 정보 포맷팅
         */
        public String getDeliveryInfo() {
            return String.format("""
                    === 협력사 계정 정보 ===
                    회사명: %s
                    담당자: %s
                    계정번호: %s
                    로그인ID: %s
                    임시비밀번호: %s
                    생성일시: %s

                    ※ 최초 로그인 후 반드시 비밀번호를 변경해주세요.
                    """,
                    companyName, contactPerson, accountNumber,
                    loginId, temporaryPassword, createdAt);
        }
    }

    /**
     * 협력사 계정 생성 요청 DTO (기존 방식 - 유지)
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
     * 협력사 계정 생성 응답 DTO (기존 방식 - 유지)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResponse {

        private Long partnerId; // 협력사 ID
        private String hierarchicalId; // 계층적 아이디 (p1-kcs01)
        private String numericAccountNumber; // 8자리 숫자 계정
        private String companyName;
        private String contactPerson;
        private String temporaryPassword; // 임시 비밀번호
        private Integer level;
        private String treePath;
        private LocalDateTime createdAt;
        private String message;

        /**
         * Entity를 CreateResponse DTO로 변환
         */
        public static CreateResponse from(Partner partner, String temporaryPassword) {
            return CreateResponse.builder()
                    .partnerId(partner.getId())
                    .hierarchicalId(partner.getAccountNumber())
                    .numericAccountNumber(partner.getNumericAccountNumber())
                    .companyName(partner.getCompanyName())
                    .contactPerson(partner.getContactPerson())
                    .temporaryPassword(temporaryPassword)
                    .level(partner.getLevel())
                    .treePath(partner.getTreePath())
                    .createdAt(partner.getCreatedAt())
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

        private Long partnerId;
        private String accountNumber;
        private String companyName;
        private String contactPerson;
        private Integer level;
        private String treePath;
        private Long parentId;
        private String status;
        private List<TreeResponse> children;

        /**
         * Entity를 TreeResponse DTO로 변환
         */
        public static TreeResponse from(Partner partner) {
            return TreeResponse.builder()
                    .partnerId(partner.getId())
                    .accountNumber(partner.getAccountNumber())
                    .companyName(partner.getCompanyName())
                    .contactPerson(partner.getContactPerson())
                    .level(partner.getLevel())
                    .treePath(partner.getTreePath())
                    .parentId(partner.getParent() != null ? partner.getParent().getId() : null)
                    .status(partner.getStatus().toString())
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

        private String reason; // 상태 변경 사유
    }
}