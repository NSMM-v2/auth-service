package com.nsmm.esg.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 본사 엔티티 - ESG 프로젝트 최상위 조직
 * 
 * 특징: 루트 권한 보유, 모든 협력사 관리
 * 계정 형태: hqAccountNumber (예: 2412161700)
 * 로그인: 이메일 주소 사용
 */
@Entity
@Table(name = "headquarters", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_hq_account_number", columnList = "hq_account_number")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Headquarters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 본사 고유 식별자

    @Column(name = "hq_account_number", unique = true, length = 10)
    private String hqAccountNumber; // 본사 계정 번호 (2412161700)

    @Column(name = "company_name", nullable = false)
    private String companyName; // 회사명

    @Column(name = "email", nullable = false, unique = true)
    private String email; // 로그인 ID

    @Column(name = "password", nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 담당자명

    @Column(name = "department", length = 100)
    private String department; // 부서

    @Column(name = "position", length = 50)
    private String position; // 직급

    @Column(name = "phone", length = 20)
    private String phone; // 연락처

    @Column(name = "address", columnDefinition = "TEXT")
    private String address; // 주소

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.ACTIVE; // 회사 상태

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 일시

    /**
     * 회사 상태 열거형
     * ACTIVE: 활성, INACTIVE: 비활성, SUSPENDED: 정지
     */
    public enum CompanyStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    /**
     * 본사 정보 업데이트 (불변성 보장)
     * null 값은 기존 값 유지, 이메일/비밀번호는 별도 메서드 사용
     */
    public Headquarters updateInfo(String companyName, String name, String department,
            String position, String phone, String address) {
        return Headquarters.builder()
                .id(this.id)
                .hqAccountNumber(this.hqAccountNumber)
                .companyName(companyName != null ? companyName : this.companyName)
                .email(this.email)
                .password(this.password)
                .name(name != null ? name : this.name)
                .department(department != null ? department : this.department)
                .position(position != null ? position : this.position)
                .phone(phone != null ? phone : this.phone)
                .address(address != null ? address : this.address)
                .status(this.status)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 비밀번호 변경 (불변성 보장)
     * 이미 암호화된 비밀번호를 전달받아 변경
     */
    public Headquarters changePassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 null이거나 빈 문자열일 수 없습니다.");
        }

        return Headquarters.builder()
                .id(this.id)
                .hqAccountNumber(this.hqAccountNumber)
                .companyName(this.companyName)
                .email(this.email)
                .password(newPassword)
                .name(this.name)
                .department(this.department)
                .position(this.position)
                .phone(this.phone)
                .address(this.address)
                .status(this.status)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 상태 변경 (불변성 보장)
     * 관리자 권한으로 계정 상태 변경
     */
    public Headquarters changeStatus(CompanyStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("회사 상태는 null일 수 없습니다.");
        }

        return Headquarters.builder()
                .id(this.id)
                .hqAccountNumber(this.hqAccountNumber)
                .companyName(this.companyName)
                .email(this.email)
                .password(this.password)
                .name(this.name)
                .department(this.department)
                .position(this.position)
                .phone(this.phone)
                .address(this.address)
                .status(newStatus)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 활성 상태 확인
     */
    public boolean isActive() {
        return CompanyStatus.ACTIVE.equals(this.status);
    }

    /**
     * 정지 상태 확인
     */
    public boolean isSuspended() {
        return CompanyStatus.SUSPENDED.equals(this.status);
    }
}
