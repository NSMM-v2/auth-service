package com.nsmm.esg.auth_service.repository;

import com.nsmm.esg.auth_service.entity.Headquarters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 본사 레포지토리
 */
@Repository
public interface HeadquartersRepository extends JpaRepository<Headquarters, Long> {

    /**
     * 이메일로 본사 조회
     */
    Optional<Headquarters> findByEmail(String email);

    /**
     * 이메일 중복 확인
     */
    boolean existsByEmail(String email);

    /**
     * 계정 번호 중복 확인
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * 활성 상태인 본사 조회
     */
    @Query("SELECT h FROM Headquarters h WHERE h.email = :email AND h.status = 'ACTIVE'")
    Optional<Headquarters> findActiveByEmail(@Param("email") String email);

    /**
     * 본사 ID로 계정 번호 생성을 위한 조회
     */
    @Query("SELECT h.id FROM Headquarters h WHERE h.id = :id")
    Optional<Long> findIdById(@Param("id") Long id);
}