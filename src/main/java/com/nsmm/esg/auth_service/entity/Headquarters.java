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
 * 본사 엔티티
 * 
 * ESG 프로젝트의 최상위 조직으로, 다음과 같은 특징을 가집니다:
 * - 최초 회원가입 주체이며 시스템의 루트 권한을 보유
 * - 모든 협력사 데이터 조회 및 관리 권한 보유
 * - 협력사 계정 생성 및 권한 부여 담당
 * - AWS IAM 스타일의 계층적 권한 관리 시스템의 최상위 레벨
 * 
 * 계정 번호 형식: HQ001, HQ002, ...
 * 
 * @author ESG Development Team
 * @version 1.0
 * @since 2024-12-16
 */
@Entity
@Table(name = "headquarters")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Headquarters {

    /**
     * 본사 고유 식별자
     * 자동 증가하는 기본키로 계정 번호 생성에 사용됩니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 회사명
     * 본사의 공식 회사명으로 필수 입력 항목입니다.
     * ESG 보고서 및 협력사 관리에서 식별자로 사용됩니다.
     */
    @Column(name = "company_name", nullable = false)
    private String companyName;

    /**
     * 이메일 주소
     * 로그인 ID로 사용되며 시스템 내에서 유일해야 합니다.
     * 비밀번호 재설정 및 중요 알림 발송에 사용됩니다.
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * 암호화된 비밀번호
     * BCrypt 알고리즘으로 암호화되어 저장됩니다.
     * 보안상 평문으로 저장되지 않으며 단방향 암호화를 사용합니다.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * 담당자 이름
     * 본사의 ESG 담당자 또는 시스템 관리자 이름입니다.
     * 협력사와의 소통 및 문의 처리 시 연락 담당자로 사용됩니다.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 담당자 부서
     * 담당자가 소속된 부서명입니다.
     * ESG 관련 업무 담당 부서를 명시합니다.
     */
    @Column(name = "department", length = 100)
    private String department;

    /**
     * 담당자 직급
     * 담당자의 직급 또는 직책입니다.
     * 협력사와의 소통 시 권한 수준을 파악하는데 사용됩니다.
     */
    @Column(name = "position", length = 50)
    private String position;

    /**
     * 연락처
     * 본사의 대표 전화번호 또는 담당자 연락처입니다.
     * 긴급 상황 발생 시 연락용으로 사용됩니다.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 주소
     * 본사의 사업장 주소로 TEXT 타입으로 저장됩니다.
     * ESG 보고서의 사업장 정보 및 협력사 관리에 활용됩니다.
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * 회사 상태
     * 본사 계정의 활성화 상태를 나타냅니다.
     * 기본값은 ACTIVE이며, 관리자에 의해 변경될 수 있습니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.ACTIVE;

    /**
     * 생성 일시
     * 본사 계정이 최초 생성된 시점을 기록합니다.
     * JPA Auditing을 통해 자동으로 설정되며 수정되지 않습니다.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 최종 수정 일시
     * 본사 정보가 마지막으로 수정된 시점을 기록합니다.
     * JPA Auditing을 통해 자동으로 업데이트됩니다.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 회사 상태 열거형
     * 
     * ACTIVE: 정상 운영 중인 상태 (기본값)
     * INACTIVE: 비활성화된 상태 (로그인 불가)
     * SUSPENDED: 일시 정지된 상태 (관리자에 의한 제재)
     */
    public enum CompanyStatus {
        /**
         * 활성 상태 - 정상적으로 시스템을 이용할 수 있는 상태
         */
        ACTIVE,
        
        /**
         * 비활성 상태 - 사용자가 직접 비활성화하거나 장기간 미사용으로 인한 상태
         */
        INACTIVE,
        
        /**
         * 정지 상태 - 관리자에 의해 강제로 정지된 상태
         */
        SUSPENDED
    }

    /**
     * 본사 계정 번호 생성
     * 
     * AWS IAM 스타일의 계정 번호를 생성합니다.
     * 형식: HQ{본사ID:3자리 zero-padding}
     * 예시: HQ001, HQ002, HQ010, HQ100
     * 
     * @return 생성된 계정 번호 문자열
     * @throws IllegalStateException ID가 null인 경우 발생
     */
    public String generateAccountNumber() {
        if (this.id == null) {
            throw new IllegalStateException("본사 ID가 설정되지 않았습니다. 엔티티를 먼저 저장해주세요.");
        }
        return String.format("HQ%03d", this.id);
    }

    /**
     * 본사 정보 업데이트 (불변성 보장)
     * 
     * 엔티티의 불변성을 보장하기 위해 새로운 인스턴스를 생성하여 반환합니다.
     * null 값이 전달된 필드는 기존 값을 유지합니다.
     * 이메일과 비밀번호는 보안상 이 메서드로 변경할 수 없습니다.
     * 
     * @param companyName 새로운 회사명 (null이면 기존 값 유지)
     * @param name 새로운 담당자명 (null이면 기존 값 유지)
     * @param department 새로운 부서명 (null이면 기존 값 유지)
     * @param position 새로운 직급 (null이면 기존 값 유지)
     * @param phone 새로운 연락처 (null이면 기존 값 유지)
     * @param address 새로운 주소 (null이면 기존 값 유지)
     * @return 업데이트된 새로운 Headquarters 인스턴스
     */
    public Headquarters updateInfo(String companyName, String name, String department, 
                                 String position, String phone, String address) {
        return Headquarters.builder()
                .id(this.id)
                .companyName(companyName != null ? companyName : this.companyName)
                .email(this.email)  // 이메일은 변경 불가 (보안상 이유)
                .password(this.password)  // 비밀번호는 별도 메서드로 변경
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
     * 
     * 보안을 위해 비밀번호만 별도로 변경하는 메서드입니다.
     * 전달받은 비밀번호는 이미 암호화된 상태여야 합니다.
     * 
     * @param newPassword 새로운 암호화된 비밀번호
     * @return 비밀번호가 변경된 새로운 Headquarters 인스턴스
     * @throws IllegalArgumentException 비밀번호가 null이거나 빈 문자열인 경우
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
     * 
     * 관리자에 의해 본사 계정의 상태를 변경할 때 사용됩니다.
     * 상태 변경은 시스템 로그에 기록되어야 하므로 신중하게 사용해야 합니다.
     * 
     * @param newStatus 새로운 회사 상태
     * @return 상태가 변경된 새로운 Headquarters 인스턴스
     * @throws IllegalArgumentException 상태가 null인 경우
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
     * 본사 계정이 활성 상태인지 확인
     * 
     * @return 활성 상태이면 true, 그렇지 않으면 false
     */
    public boolean isActive() {
        return CompanyStatus.ACTIVE.equals(this.status);
    }

    /**
     * 본사 계정이 정지 상태인지 확인
     * 
     * @return 정지 상태이면 true, 그렇지 않으면 false
     */
    public boolean isSuspended() {
        return CompanyStatus.SUSPENDED.equals(this.status);
    }
}
