package com.nsmm.esg.auth_service.repository;

import com.nsmm.esg.auth_service.entity.Headquarters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 본사 데이터 액세스 레이어
 * 
 * 주요 기능:
 * - 이메일 기반 조회 (로그인용)
 * - 계정 번호 기반 조회 (JWT 검증용)
 * - UUID 기반 조회 (API 연동용)
 * - 활성 상태 필터링
 * - 계정번호 생성 지원
 */
@Repository
public interface HeadquartersRepository extends JpaRepository<Headquarters, Long> {

    /**
     * UUID로 본사 조회
     */
    Optional<Headquarters> findByUuid(String uuid);

    /**
     * UUID 중복 확인
     */
    boolean existsByUuid(String uuid);

    // === 기존 조회 메서드들 ===

    /**
     * 이메일로 본사 조회 (로그인용)
     */
    Optional<Headquarters> findByEmail(String email);

    /**
     * 이메일 중복 확인
     */
    boolean existsByEmail(String email);

    /**
     * 계정 번호 중복 확인
     */
    boolean existsByHqAccountNumber(String hqAccountNumber);

    /**
     * 특정 패턴으로 시작하는 계정번호를 가진 본사 수 조회
     * 계정번호 생성 시 사용 (예: HQ20241216으로 시작하는 본사 수)
     */
    @Query("SELECT COUNT(h) FROM Headquarters h WHERE h.hqAccountNumber LIKE CONCAT(:pattern, '%')")
    long countByHqAccountNumberStartingWith(@Param("pattern") String pattern);

    /**
     * 본사 개수 조회 (순번 생성용)
     */
    @Query("SELECT COUNT(h) FROM Headquarters h")
    long countAll();
}