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
import java.util.concurrent.ThreadLocalRandom;

/**
 * 본사 엔티티 - ESG 프로젝트 최상위 조직
 * 
 * 특징: 루트 권한 보유, 모든 협력사 관리, 8자리 숫자 계정 번호
 * 계정 형태: 8자리 숫자 (예: 10000001, 10000002, ...)
 * 로그인: 이메일 주소 사용
 */
@Entity
@Table(name = "headquarters", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_account_number", columnList = "account_number")
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

    @Column(name = "account_number", unique = true, length = 8)
    private String accountNumber; // 8자리 숫자 계정 번호 (10000001)

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
     * 본사 8자리 숫자 계정 번호 생성 (규칙적이지만 예측 어려운 형태)
     * 
     * 규칙:
     * - 첫 2자리: 본사 구분 코드 (10~19)
     * - 중간 4자리: 연도 + 월 (2024년 12월 → 2412)
     * - 마지막 2자리: 랜덤 (00~99)
     * 
     * 예시: 12241201, 15241203, 18241212
     * 
     * @return 8자리 숫자 문자열
     */
    public static String generateNewAccountNumber() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 첫 2자리: 본사 구분 코드 (10~19)
        int companyCode = random.nextInt(10, 20);

        // 중간 4자리: 현재 연도 마지막 2자리 + 월 + 일의 일부
        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear() % 100; // 24 (2024년)
        int month = now.getMonthValue(); // 01~12
        String yearMonth = String.format("%02d%02d", year, month);

        // 마지막 2자리: 랜덤
        int randomSuffix = random.nextInt(0, 100);

        return companyCode + yearMonth + String.format("%02d", randomSuffix);
    }

    /**
     * 본사 계정 번호 반환 (인스턴스 메서드)
     * 저장된 계정 번호가 있으면 반환, 없으면 ID 기반으로 생성
     */
    public String generateAccountNumber() {
        if (this.accountNumber != null) {
            return this.accountNumber;
        }
        // 기존 방식 호환성을 위해 유지 (권장하지 않음)
        if (this.id == null) {
            throw new IllegalStateException("본사 ID가 설정되지 않았습니다.");
        }
        return String.format("%08d", 10000000 + this.id); // 10000001, 10000002, ...
    }

    /**
     * 본사 정보 업데이트 (불변성 보장)
     * null 값은 기존 값 유지, 이메일/비밀번호는 별도 메서드 사용
     */
    public Headquarters updateInfo(String companyName, String name, String department,
            String position, String phone, String address) {
        return Headquarters.builder()
                .id(this.id)
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
