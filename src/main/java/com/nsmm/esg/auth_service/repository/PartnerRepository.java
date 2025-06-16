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
 * - 계층적 아이디 기반 조회 (로그인용)
 * - 트리 구조 조회 (권한 관리용)
 * - 본사별 협력사 관리
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

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
        * 본사별 1차 협력사 조회 (parent가 null인 협력사)
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.id = :headquartersId AND p.parent IS NULL ORDER BY p.createdAt ASC")
       List<Partner> findFirstLevelPartnersByHeadquarters(@Param("headquartersId") Long headquartersId);

       /**
        * 특정 협력사의 직접 하위 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.parent.id = :parentId ORDER BY p.createdAt ASC")
       List<Partner> findDirectChildrenByParentId(@Param("parentId") Long parentId);

       /**
        * 트리 경로로 하위 협력사 조회 (자신 포함)
        */
       @Query("SELECT p FROM Partner p WHERE p.treePath LIKE CONCAT(:treePath, '%') ORDER BY p.level ASC, p.createdAt ASC")
       List<Partner> findByTreePathStartingWith(@Param("treePath") String treePath);

       /**
        * 특정 레벨의 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.id = :headquartersId AND p.level = :level ORDER BY p.createdAt ASC")
       List<Partner> findByHeadquartersAndLevel(@Param("headquartersId") Long headquartersId,
                     @Param("level") Integer level);

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
        * 본사별 총 협력사 수 조회
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.headquarters.id = :headquartersId")
       long countByHeadquartersId(@Param("headquartersId") Long headquartersId);

       /**
        * 본사별 레벨별 협력사 수 조회
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.headquarters.id = :headquartersId AND p.level = :level")
       long countByHeadquartersIdAndLevel(@Param("headquartersId") Long headquartersId, @Param("level") Integer level);

       /**
        * 특정 협력사의 모든 하위 협력사 수 조회
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.treePath LIKE CONCAT(:treePath, '%') AND p.id != :excludeId")
       long countDescendantsByTreePath(@Param("treePath") String treePath, @Param("excludeId") Long excludeId);

       // === 상태별 조회 메서드 ===

       /**
        * 활성 상태 협력사만 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.id = :headquartersId AND p.status = 'ACTIVE' ORDER BY p.level ASC, p.createdAt ASC")
       List<Partner> findActivePartnersByHeadquarters(@Param("headquartersId") Long headquartersId);

       /**
        * 비밀번호 미변경 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.id = :headquartersId AND p.passwordChanged = false ORDER BY p.createdAt ASC")
       List<Partner> findUnchangedPasswordPartners(@Param("headquartersId") Long headquartersId);
}