package com.nsmm.esg.auth_service.repository;

import com.nsmm.esg.auth_service.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 협력사 데이터 액세스 레이어
 * 
 * 주요 기능:
 * - UUID 기반 조회 (프론트엔드 요구사항)
 * - 계층적 아이디 기반 조회 (로그인용)
 * - 트리 구조 조회 (권한 관리용 - 본인 + 직속 하위 1단계)
 * - 본사별 협력사 관리
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

       // === UUID 기반 조회 (프론트엔드 요구사항) ===

       /**
        * UUID로 협력사 조회
        */
       Optional<Partner> findByUuid(String uuid);

       /**
        * UUID 중복 확인
        */
       boolean existsByUuid(String uuid);

       // === 로그인 관련 메서드 ===

       /**
        * 본사 계정번호 + 계층적 아이디로 협력사 조회 (로그인용)
        */
       Optional<Partner> findByHqAccountNumberAndHierarchicalId(String hqAccountNumber, String hierarchicalId);

       /**
        * 이메일로 협력사 조회 (고유 식별용)
        */
       Optional<Partner> findByEmail(String email);

       // === 계층 구조 관리 메서드 ===

       /**
        * 본사별 1차 협력사 조회 (parentPartner가 null인 협력사)
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.headquartersId = :headquartersId AND p.parentPartner IS NULL ORDER BY p.createdAt ASC")
       List<Partner> findFirstLevelPartnersByHeadquarters(@Param("headquartersId") Long headquartersId);

       /**
        * 특정 협력사의 직접 하위 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.parentPartner.partnerId = :parentPartnerId ORDER BY p.createdAt ASC")
       List<Partner> findDirectChildrenByParentId(@Param("parentPartnerId") Long parentPartnerId);

       /**
        * 권한 제어: 본인 + 직속 하위 1단계만 조회
        * 예: 1차 협력사가 조회하면 본인(L1-001) + 2차(L1-001/L2-*)만 반환
        */
       @Query("SELECT p FROM Partner p WHERE " +
                     "(p.treePath = :currentTreePath) OR " +
                     "(p.treePath LIKE CONCAT(:currentTreePath, 'L', :directChildLevel, '-%') AND " +
                     "LENGTH(p.treePath) - LENGTH(REPLACE(p.treePath, '/', '')) = :expectedSlashCount)")
       List<Partner> findAccessiblePartners(@Param("currentTreePath") String currentTreePath,
                     @Param("directChildLevel") Integer directChildLevel,
                     @Param("expectedSlashCount") Integer expectedSlashCount);

       // === 중복 검사 메서드 ===

       /**
        * 이메일 중복 확인
        */
       boolean existsByEmail(String email);

       /**
        * 본사 내 계층적 아이디 중복 확인
        */
       boolean existsByHqAccountNumberAndHierarchicalId(String hqAccountNumber, String hierarchicalId);

       // === 통계 조회 메서드 ===

       /**
        * 본사별 레벨별 협력사 수 조회
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.headquarters.headquartersId = :headquartersId AND p.level = :level")
       long countByHeadquartersIdAndLevel(@Param("headquartersId") Long headquartersId, @Param("level") Integer level);

       // === 상태별 조회 메서드 ===

       /**
        * 비밀번호 미변경 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.headquartersId = :headquartersId AND p.passwordChanged = false ORDER BY p.createdAt ASC")
       List<Partner> findUnchangedPasswordPartners(@Param("headquartersId") Long headquartersId);

       // === 권한별 조회 메서드 (본사용) ===

       /**
        * 본사가 모든 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.headquartersId = :headquartersId ORDER BY p.level ASC, p.createdAt ASC")
       List<Partner> findAllPartnersByHeadquarters(@Param("headquartersId") Long headquartersId);

       // === 계층적 ID 생성 지원 메서드 ===

       /**
        * 특정 본사 + 레벨에서 다음 순번 계산용
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.headquarters.headquartersId = :headquartersId AND p.level = :level")
       long countByHeadquartersAndLevel(@Param("headquartersId") Long headquartersId, @Param("level") Integer level);
}