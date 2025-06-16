package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.dto.AuthDto;
import com.nsmm.esg.auth_service.dto.HeadquartersDto;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.repository.HeadquartersRepository;
import com.nsmm.esg.auth_service.util.JwtUtil;
import com.nsmm.esg.auth_service.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본사 서비스
 * 본사 회원가입, 로그인, 정보 관리 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HeadquartersService {

    private final HeadquartersRepository headquartersRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    /**
     * 본사 회원가입
     */
    @Transactional
    public HeadquartersDto.Response signup(HeadquartersDto.SignupRequest request) {
        log.info("본사 회원가입 시작: {}", request.getEmail());

        // 이메일 중복 확인
        if (headquartersRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        // 비밀번호 강도 검증
        if (!passwordUtil.isValidPassword(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 대문자, 소문자, 숫자, 특수문자를 각각 포함해야 합니다.");
        }

        // 본사 엔티티 생성
        Headquarters headquarters = Headquarters.builder()
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .password(passwordUtil.encodePassword(request.getPassword()))
                .name(request.getName())
                .department(request.getDepartment())
                .position(request.getPosition())
                .phone(request.getPhone())
                .address(request.getAddress())
                .status(Headquarters.CompanyStatus.ACTIVE)
                .build();

        // 저장
        Headquarters savedHeadquarters = headquartersRepository.save(headquarters);
        
        log.info("본사 회원가입 완료: {} (ID: {})", savedHeadquarters.getEmail(), savedHeadquarters.getId());
        
        return HeadquartersDto.Response.from(savedHeadquarters);
    }

    /**
     * 본사 로그인
     */
    public AuthDto.TokenResponse login(HeadquartersDto.LoginRequest request) {
        log.info("본사 로그인 시도: {}", request.getEmail());

        // 활성 상태인 본사 조회
        Headquarters headquarters = headquartersRepository.findActiveByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 비활성화된 계정입니다: " + request.getEmail()));

        // 비밀번호 검증
        if (!passwordUtil.matches(request.getPassword(), headquarters.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 클레임 생성
        AuthDto.JwtClaims claims = AuthDto.JwtClaims.builder()
                .accountNumber(headquarters.generateAccountNumber())
                .companyName(headquarters.getCompanyName())
                .userType("HEADQUARTERS")
                .level(null)  // 본사는 레벨 없음
                .treePath(null)  // 본사는 트리 경로 없음
                .headquartersId(headquarters.getId())
                .userId(headquarters.getId())
                .build();

        // 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(claims);
        String refreshToken = jwtUtil.generateRefreshToken(headquarters.generateAccountNumber());

        log.info("본사 로그인 성공: {} (계정번호: {})", headquarters.getEmail(), headquarters.generateAccountNumber());

        return AuthDto.TokenResponse.of(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                headquarters.generateAccountNumber(),
                headquarters.getCompanyName(),
                "HEADQUARTERS",
                null
        );
    }

    /**
     * 본사 정보 조회
     */
    public HeadquartersDto.Response getHeadquartersInfo(Long headquartersId) {
        log.info("본사 정보 조회: {}", headquartersId);

        Headquarters headquarters = headquartersRepository.findById(headquartersId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + headquartersId));

        return HeadquartersDto.Response.from(headquarters);
    }

    /**
     * 본사 정보 수정
     */
    @Transactional
    public HeadquartersDto.Response updateHeadquartersInfo(Long headquartersId, HeadquartersDto.UpdateRequest request) {
        log.info("본사 정보 수정: {}", headquartersId);

        Headquarters headquarters = headquartersRepository.findById(headquartersId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + headquartersId));

        // 불변성을 보장하는 업데이트
        Headquarters updatedHeadquarters = headquarters.updateInfo(
                request.getCompanyName(),
                request.getName(),
                request.getDepartment(),
                request.getPosition(),
                request.getPhone(),
                request.getAddress()
        );

        Headquarters savedHeadquarters = headquartersRepository.save(updatedHeadquarters);
        
        log.info("본사 정보 수정 완료: {}", savedHeadquarters.getId());
        
        return HeadquartersDto.Response.from(savedHeadquarters);
    }

    /**
     * 이메일 중복 확인
     */
    public boolean isEmailExists(String email) {
        return headquartersRepository.existsByEmail(email);
    }

    /**
     * 본사 상태 변경 (관리자용)
     */
    @Transactional
    public void changeStatus(Long headquartersId, Headquarters.CompanyStatus status) {
        log.info("본사 상태 변경: {} -> {}", headquartersId, status);

        Headquarters headquarters = headquartersRepository.findById(headquartersId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + headquartersId));

        Headquarters updatedHeadquarters = headquarters.changeStatus(status);
        headquartersRepository.save(updatedHeadquarters);
        
        log.info("본사 상태 변경 완료: {} -> {}", headquartersId, status);
    }

    /**
     * 계정 번호로 본사 조회 (내부용)
     */
    public Headquarters findByAccountNumber(String accountNumber) {
        // 계정 번호에서 ID 추출 (HQ001 -> 1)
        try {
            String idStr = accountNumber.substring(2); // "HQ" 제거
            Long id = Long.parseLong(idStr);
            return headquartersRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + accountNumber));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 계정 번호 형식입니다: " + accountNumber);
        }
    }
} 