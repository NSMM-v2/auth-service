package com.nsmm.esg.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 협력사 엔티티 (계층형 구조)
 * 
 * DART API 기반 요구사항 반영:
 * - UUID: DART API에서 제공하는 회사 고유 식별자 (접두사 없는 순수 UUID)
 * - contactPerson: DART API에서 제공하는 대표자명
 * - companyName: DART API에서 제공하는 회사명
 * - address: DART API에서 제공하는 회사 주소
 * - email: 선택적 필드 (초기에는 null 가능)
 * 
 * 계층적 구조:
 * - 계층적 ID: L{레벨}-{순번} (L1-001, L2-001, L3-001...)
 * - 트리 경로: /{본사ID}/L{레벨}-{순번}/ (/{본사ID}/L1-001/L2-001/...)
 * - 초기 비밀번호: 계층적 ID와 동일
 * 
 * 권한 제어: 본인 + 직속 하위 1단계만 접근 가능
 */
@Entity
@Table(name = "partners", indexes = {
        @Index(name = "idx_partner_uuid", columnList = "partner_uuid"),
        @Index(name = "idx_parent_partner_id", columnList = "parent_partner_id"),
        @Index(name = "idx_tree_path", columnList = "tree_path"),
        @Index(name = "idx_level", columnList = "level"),
        @Index(name = "idx_headquarters_id", columnList = "headquarters_id"),
        @Index(name = "idx_hq_account_hierarchical", columnList = "hq_account_number,hierarchical_id")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Long partnerId; // 협력사 고유 식별자

    @Column(name = "partner_uuid", unique = true, nullable = false, length = 36)
    private String uuid; // DART API에서 제공하는 회사 UUID (접두사 없는 순수 UUID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "headquarters_id", nullable = false)
    private Headquarters headquarters; // 소속 본사 (필수)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_partner_id")
    private Partner parentPartner; // 상위 협력사 (1차 협력사는 null)

    @OneToMany(mappedBy = "parentPartner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Partner> childPartners = new ArrayList<>(); // 하위 협력사 목록

    @Column(name = "hq_account_number", nullable = false, length = 10)
    private String hqAccountNumber; // 본사 계정 번호 (2412161700)

    @Column(name = "hierarchical_id", nullable = false, length = 20)
    private String hierarchicalId; // 계층적 아이디 (L1-001, L2-001, L3-001...)

    @Column(name = "company_name", nullable = false)
    private String companyName; // 협력사명

    @Column(name = "email", unique = true)
    private String email; // 이메일 (선택적, 초기에는 null 가능)

    @Column(name = "password", nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson; // 대표자명 (담당자명)

    @Column(name = "phone", length = 20)
    private String phone; // 연락처

    @Column(name = "address", columnDefinition = "TEXT")
    private String address; // 주소

    @Column(name = "level", nullable = false)
    private Integer level; // 협력사 계층 레벨 (1차=1, 2차=2, ...)

    @Column(name = "tree_path", nullable = false, length = 500)
    private String treePath; // 트리 경로 (/{본사ID}/L1-001/L2-001/...)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.ACTIVE; // 협력사 상태

    @Column(name = "password_changed", nullable = false)
    @Builder.Default
    private Boolean passwordChanged = false; // 초기 비밀번호 변경 여부

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 일시

    /**
     * 협력사 상태 열거형
     */
    public enum PartnerStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING
    }

    /**
     * 전체 계정 번호 반환 (본사계정번호-계층적아이디)
     * 예: 2412161700-L1-001, 2412161700-L2-001
     */
    public String getFullAccountNumber() {
        return this.hqAccountNumber + "-" + this.hierarchicalId;
    }

    /**
     * 협력사 정보 업데이트 (불변성 보장)
     * null 값은 기존 값 유지
     */
    public Partner updateInfo(String companyName, String contactPerson, String phone, String address) {
        return Partner.builder()
                .partnerId(this.partnerId)
                .uuid(this.uuid)
                .headquarters(this.headquarters)
                .parentPartner(this.parentPartner)
                .childPartners(this.childPartners)
                .hqAccountNumber(this.hqAccountNumber)
                .hierarchicalId(this.hierarchicalId)
                .companyName(companyName != null ? companyName : this.companyName)
                .email(this.email)
                .password(this.password)
                .contactPerson(contactPerson != null ? contactPerson : this.contactPerson)
                .phone(phone != null ? phone : this.phone)
                .address(address != null ? address : this.address)
                .level(this.level)
                .treePath(this.treePath)
                .status(this.status)
                .passwordChanged(this.passwordChanged)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 이메일 설정 (불변성 보장)
     * 초기에는 null이었다가 나중에 이메일 등록할 때 사용
     */
    public Partner setEmail(String email) {
        return Partner.builder()
                .partnerId(this.partnerId)
                .uuid(this.uuid)
                .headquarters(this.headquarters)
                .parentPartner(this.parentPartner)
                .childPartners(this.childPartners)
                .hqAccountNumber(this.hqAccountNumber)
                .hierarchicalId(this.hierarchicalId)
                .companyName(this.companyName)
                .email(email)
                .password(this.password)
                .contactPerson(this.contactPerson)
                .phone(this.phone)
                .address(this.address)
                .level(this.level)
                .treePath(this.treePath)
                .status(this.status)
                .passwordChanged(this.passwordChanged)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 비밀번호 변경 (불변성 보장)
     */
    public Partner changePassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 null이거나 빈 문자열일 수 없습니다.");
        }

        return Partner.builder()
                .partnerId(this.partnerId)
                .uuid(this.uuid)
                .headquarters(this.headquarters)
                .parentPartner(this.parentPartner)
                .childPartners(this.childPartners)
                .hqAccountNumber(this.hqAccountNumber)
                .hierarchicalId(this.hierarchicalId)
                .companyName(this.companyName)
                .email(this.email)
                .password(newPassword)
                .contactPerson(this.contactPerson)
                .phone(this.phone)
                .address(this.address)
                .level(this.level)
                .treePath(this.treePath)
                .status(this.status)
                .passwordChanged(true) // 비밀번호 변경 시 true로 설정
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 상태 변경 (불변성 보장)
     */
    public Partner changeStatus(PartnerStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("협력사 상태는 null일 수 없습니다.");
        }

        return Partner.builder()
                .partnerId(this.partnerId)
                .uuid(this.uuid)
                .headquarters(this.headquarters)
                .parentPartner(this.parentPartner)
                .childPartners(this.childPartners)
                .hqAccountNumber(this.hqAccountNumber)
                .hierarchicalId(this.hierarchicalId)
                .companyName(this.companyName)
                .email(this.email)
                .password(this.password)
                .contactPerson(this.contactPerson)
                .phone(this.phone)
                .address(this.address)
                .level(this.level)
                .treePath(this.treePath)
                .status(newStatus)
                .passwordChanged(this.passwordChanged)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 활성 상태 확인
     */
    public boolean isActive() {
        return PartnerStatus.ACTIVE.equals(this.status);
    }

    /**
     * 초기 비밀번호 사용 중인지 확인
     */
    public boolean isUsingInitialPassword() {
        return !this.passwordChanged;
    }

    /**
     * 직속 하위 협력사 레벨 반환 (권한 제어용)
     * 예: 1차 협력사 → 2차, 2차 협력사 → 3차
     */
    public Integer getDirectChildLevel() {
        return this.level + 1;
    }
}