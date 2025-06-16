package com.nsmm.esg.auth_service.repository;

import com.nsmm.esg.auth_service.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 협력사 레포지토리 (계층 구조 + AWS 스타일 계정 지원)
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

       /**
        * 계정 번호로 협력사 조회
        */
       Optional<Partner> findByAccountNumber(String accountNumber);

       /**
        * 이메일로 협력사 조회
        */
       Optional<Partner> findByEmail(String email);

       /**
        * 계정 번호 중복 확인
        */
       boolean existsByAccountNumber(String accountNumber);

       /**
        * 이메일 중복 확인
        */
       boolean existsByEmail(String email);

       /**
        * 활성 상태인 협력사 조회 (계정 번호)
        */
       @Query("SELECT p FROM Partner p WHERE p.accountNumber = :accountNumber AND p.status = 'ACTIVE'")
       Optional<Partner> findActiveByAccountNumber(@Param("accountNumber") String accountNumber);

       /**
        * 외부 파트너 ID로 협력사 조회
        */
       Optional<Partner> findByExternalPartnerId(Long externalPartnerId);

       /**
        * 외부 파트너 ID 중복 확인
        */
       boolean existsByExternalPartnerId(Long externalPartnerId);

       /**
        * 숫자 계정 번호로 협력사 조회
        */
       Optional<Partner> findByNumericAccountNumber(String numericAccountNumber);

       /**
        * 숫자 계정 번호 중복 확인
        */
       boolean existsByNumericAccountNumber(String numericAccountNumber);

       /**
        * 활성 상태인 협력사 조회 (숫자 계정 번호)
        */
       @Query("SELECT p FROM Partner p WHERE p.numericAccountNumber = :numericAccountNumber AND p.status = 'ACTIVE'")
       Optional<Partner> findActiveByNumericAccountNumber(@Param("numericAccountNumber") String numericAccountNumber);

       /**
        * 본사별 1차 협력사 목록 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.id = :headquartersId AND p.parent IS NULL ORDER BY p.createdAt")
       List<Partner> findTopLevelPartnersByHeadquarters(@Param("headquartersId") Long headquartersId);

       /**
        * 상위 협력사의 직접 하위 협력사 목록 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.parent.id = :parentId ORDER BY p.createdAt")
       List<Partner> findDirectChildrenByParent(@Param("parentId") Long parentId);

       /**
        * 트리 경로를 이용한 모든 하위 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.treePath LIKE CONCAT(:treePath, '%') AND p.id != :excludeId ORDER BY p.level, p.createdAt")
       List<Partner> findAllDescendantsByTreePath(@Param("treePath") String treePath,
                     @Param("excludeId") Long excludeId);

       /**
        * 특정 레벨의 협력사 목록 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.headquarters.id = :headquartersId AND p.level = :level ORDER BY p.createdAt")
       List<Partner> findByHeadquartersAndLevel(@Param("headquartersId") Long headquartersId,
                     @Param("level") Integer level);

       /**
        * 본사별 전체 협력사 수 조회
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.headquarters.id = :headquartersId")
       Long countByHeadquarters(@Param("headquartersId") Long headquartersId);

       /**
        * 상위 협력사별 하위 협력사 수 조회
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.parent.id = :parentId")
       Long countByParent(@Param("parentId") Long parentId);

       /**
        * 본사와 상위 협력사로 다음 순번 조회 (계정 번호 생성용)
        */
       @Query("SELECT COUNT(p) + 1 FROM Partner p WHERE p.headquarters.id = :headquartersId AND " +
                     "(:parentId IS NULL AND p.parent IS NULL OR p.parent.id = :parentId)")
       Integer getNextSequenceNumber(@Param("headquartersId") Long headquartersId, @Param("parentId") Long parentId);

       /**
        * 협력사의 모든 상위 협력사 조회 (트리 경로 역순)
        */
       @Query("SELECT p FROM Partner p WHERE p.treePath IN " +
                     "(SELECT SUBSTRING(:treePath, 1, LOCATE('/', :treePath, 2)) FROM Partner p2 WHERE p2.id = :partnerId) "
                     +
                     "ORDER BY p.level")
       List<Partner> findAllAncestorsByTreePath(@Param("treePath") String treePath, @Param("partnerId") Long partnerId);

       /**
        * 본사별 트리 구조 전체 조회 (1차 협력사만, 하위는 fetch join으로)
        */
       @Query("SELECT DISTINCT p FROM Partner p " +
                     "LEFT JOIN FETCH p.children " +
                     "WHERE p.headquarters.id = :headquartersId AND p.parent IS NULL " +
                     "ORDER BY p.createdAt")
       List<Partner> findTreeStructureByHeadquarters(@Param("headquartersId") Long headquartersId);

       /**
        * 특정 협력사의 하위 트리 구조 조회
        */
       @Query("SELECT DISTINCT p FROM Partner p " +
                     "LEFT JOIN FETCH p.children " +
                     "WHERE p.treePath LIKE CONCAT(:treePath, '%') " +
                     "ORDER BY p.level, p.createdAt")
       List<Partner> findSubTreeByTreePath(@Param("treePath") String treePath);

       /**
        * 비밀번호 변경이 필요한 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.passwordChanged = false AND p.status = 'ACTIVE'")
       List<Partner> findPartnersNeedingPasswordChange();

       /**
        * 임시 비밀번호로 생성된 협력사 조회
        */
       @Query("SELECT p FROM Partner p WHERE p.temporaryPassword IS NOT NULL AND p.passwordChanged = false")
       List<Partner> findPartnersWithTemporaryPassword();

       /**
        * 계층적 아이디 중복 체크용 - 특정 패턴으로 시작하는 계정번호 개수 조회
        * 
        * @param pattern  검색할 패턴 (예: "p1-kcs%")
        * @param level    협력사 레벨
        * @param parentId 상위 협력사 ID (null이면 1차 협력사)
        * @return 해당 패턴의 개수
        */
       @Query("SELECT COUNT(p) FROM Partner p WHERE p.accountNumber LIKE :pattern AND p.level = :level AND " +
                     "(:parentId IS NULL AND p.parent IS NULL OR p.parent.id = :parentId)")
       Long countByAccountNumberPatternAndLevelAndParent(@Param("pattern") String pattern,
                     @Param("level") Integer level,
                     @Param("parentId") Long parentId);

       /**
        * 같은 이니셜을 가진 협력사들의 다음 순번 조회
        * 
        * @param initialsPattern 이니셜 패턴 (예: "p1-kcs%")
        * @param level           협력사 레벨
        * @param parentId        상위 협력사 ID
        * @return 다음 순번
        */
       @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.accountNumber, LENGTH(:initialsPattern) + 1) AS int)), 0) + 1 " +
                     "FROM Partner p WHERE p.accountNumber LIKE :initialsPattern AND p.level = :level AND " +
                     "(:parentId IS NULL AND p.parent IS NULL OR p.parent.id = :parentId)")
       Integer getNextSequenceForInitials(@Param("initialsPattern") String initialsPattern,
                     @Param("level") Integer level,
                     @Param("parentId") Long parentId);
}