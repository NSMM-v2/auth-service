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
import java.util.ArrayList;
import java.util.List;

/**
 * 협력사 엔티티 (계층 구조)
 * 1차, 2차, 3차... N차 협력사를 지원하는 트리 구조
 */
@Entity
@Table(name = "partners", indexes = {
    @Index(name = "idx_parent_id", columnList = "parent_id"),
    @Index(name = "idx_tree_path", columnList = "tree_path"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_headquarters_id", columnList = "headquarters_id")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "headquarters_id", nullable = false)
    private Headquarters headquarters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Partner parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Partner> children = new ArrayList<>();

    @Column(name = "account_number", nullable = false, unique = true, length = 100)
    private String accountNumber;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "tree_path", nullable = false, length = 500)
    private String treePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.ACTIVE;

    @Column(name = "password_changed", nullable = false)
    @Builder.Default
    private Boolean passwordChanged = false;

    @Column(name = "temporary_password", length = 100)
    private String temporaryPassword;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 협력사 상태
     */
    public enum PartnerStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING
    }

    /**
     * 계층적 계정 번호 생성
     * 예: HQ001-L1-001, HQ001-L1-001-L2-001
     */
    public String generateAccountNumber(String hqAccountNumber, int sequenceNumber) {
        if (parent == null) {
            // 1차 협력사
            return String.format("%s-L1-%03d", hqAccountNumber, sequenceNumber);
        } else {
            // 2차 이상 협력사
            return String.format("%s-L%d-%03d", parent.getAccountNumber(), level, sequenceNumber);
        }
    }

    /**
     * 트리 경로 생성
     * 형식: /parent_id/current_id/
     */
    public String generateTreePath() {
        if (parent == null) {
            return String.format("/%d/", this.id);
        } else {
            return parent.getTreePath() + this.id + "/";
        }
    }

    /**
     * 하위 협력사인지 확인
     */
    public boolean isDescendantOf(Partner ancestor) {
        return this.treePath.startsWith(ancestor.getTreePath());
    }

    /**
     * 최상위 협력사인지 확인 (1차 협력사)
     */
    public boolean isTopLevel() {
        return this.parent == null;
    }

    /**
     * 협력사 정보 업데이트 (불변성 보장)
     */
    public Partner updateInfo(String companyName, String contactPerson, String phone, String address) {
        return Partner.builder()
                .id(this.id)
                .headquarters(this.headquarters)
                .parent(this.parent)
                .children(this.children)
                .accountNumber(this.accountNumber)
                .companyName(companyName != null ? companyName : this.companyName)
                .email(this.email)  // 이메일은 변경 불가
                .password(this.password)  // 비밀번호는 별도 메서드로 변경
                .contactPerson(contactPerson != null ? contactPerson : this.contactPerson)
                .phone(phone != null ? phone : this.phone)
                .address(address != null ? address : this.address)
                .level(this.level)
                .treePath(this.treePath)
                .status(this.status)
                .passwordChanged(this.passwordChanged)
                .temporaryPassword(this.temporaryPassword)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 비밀번호 변경 (불변성 보장)
     */
    public Partner changePassword(String newPassword) {
        return Partner.builder()
                .id(this.id)
                .headquarters(this.headquarters)
                .parent(this.parent)
                .children(this.children)
                .accountNumber(this.accountNumber)
                .companyName(this.companyName)
                .email(this.email)
                .password(newPassword)
                .contactPerson(this.contactPerson)
                .phone(this.phone)
                .address(this.address)
                .level(this.level)
                .treePath(this.treePath)
                .status(this.status)
                .passwordChanged(true)
                .temporaryPassword(null)  // 임시 비밀번호 제거
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 상태 변경 (불변성 보장)
     */
    public Partner changeStatus(PartnerStatus newStatus) {
        return Partner.builder()
                .id(this.id)
                .headquarters(this.headquarters)
                .parent(this.parent)
                .children(this.children)
                .accountNumber(this.accountNumber)
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
                .temporaryPassword(this.temporaryPassword)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 계정 번호와 트리 경로 설정 (생성 시에만 사용)
     */
    public Partner withAccountNumberAndTreePath(String accountNumber, String treePath) {
        return Partner.builder()
                .id(this.id)
                .headquarters(this.headquarters)
                .parent(this.parent)
                .children(this.children)
                .accountNumber(accountNumber)
                .companyName(this.companyName)
                .email(this.email)
                .password(this.password)
                .contactPerson(this.contactPerson)
                .phone(this.phone)
                .address(this.address)
                .level(this.level)
                .treePath(treePath)
                .status(this.status)
                .passwordChanged(this.passwordChanged)
                .temporaryPassword(this.temporaryPassword)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 상태를 ACTIVE로 변경 (생성 완료 시 사용)
     */
    public Partner activate() {
        return Partner.builder()
                .id(this.id)
                .headquarters(this.headquarters)
                .parent(this.parent)
                .children(this.children)
                .accountNumber(this.accountNumber)
                .companyName(this.companyName)
                .email(this.email)
                .password(this.password)
                .contactPerson(this.contactPerson)
                .phone(this.phone)
                .address(this.address)
                .level(this.level)
                .treePath(this.treePath)
                .status(PartnerStatus.ACTIVE)
                .passwordChanged(this.passwordChanged)
                .temporaryPassword(this.temporaryPassword)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
} 