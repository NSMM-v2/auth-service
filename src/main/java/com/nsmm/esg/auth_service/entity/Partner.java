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
 * 협력사 엔티티 (순수한 데이터 모델)
 * 
 * 비즈니스 로직은 전용 서비스 클래스로 분리:
 * - PartnerAccountService: 계정 번호 생성
 * - PartnerTreeService: 트리 구조 관리
 * - PartnerFactoryService: 엔티티 생성/수정
 */
@Entity
@Table(name = "partners", indexes = {
        @Index(name = "idx_parent_id", columnList = "parent_id"),
        @Index(name = "idx_tree_path", columnList = "tree_path"),
        @Index(name = "idx_level", columnList = "level"),
        @Index(name = "idx_headquarters_id", columnList = "headquarters_id"),
        @Index(name = "idx_numeric_account_number", columnList = "numeric_account_number"),
        @Index(name = "idx_external_partner_id", columnList = "external_partner_id")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 협력사 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "headquarters_id", nullable = false)
    private Headquarters headquarters; // 소속 본사 (필수)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Partner parent; // 상위 협력사 (1차 협력사는 null)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Partner> children = new ArrayList<>(); // 하위 협력사 목록

    @Column(name = "account_number", nullable = false, unique = true, length = 100)
    private String accountNumber; // 계층적 계정 번호 (p1-kcs01, p2-lyh01)

    @Column(name = "external_partner_id")
    private Long externalPartnerId; // 외부 시스템 파트너 ID

    @Column(name = "numeric_account_number", unique = true, length = 12)
    private String numericAccountNumber; // 8자리 숫자 계정 (90541842)

    @Column(name = "company_name", nullable = false)
    private String companyName; // 협력사명

    @Column(name = "email", nullable = false, unique = true)
    private String email; // 로그인 ID (이메일 형식)

    @Column(name = "password", nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson; // 담당자명

    @Column(name = "phone", length = 20)
    private String phone; // 연락처

    @Column(name = "address", columnDefinition = "TEXT")
    private String address; // 주소

    @Column(name = "level", nullable = false)
    private Integer level; // 협력사 계층 레벨 (1차=1, 2차=2, ...)

    @Column(name = "tree_path", nullable = false, length = 500)
    private String treePath; // 트리 경로 (/parent_id/current_id/)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.ACTIVE; // 협력사 상태

    @Column(name = "password_changed", nullable = false)
    @Builder.Default
    private Boolean passwordChanged = false; // 임시 비밀번호 변경 여부

    @Column(name = "temporary_password", length = 100)
    private String temporaryPassword; // 임시 비밀번호 (생성 시에만 사용)

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
}