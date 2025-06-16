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
    public HeadquartersDto.SignupResponse signup(HeadquartersDto.SignupRequest request) {
        log.info("본사 회원가입 시작: {}", request.getEmail());

        // 이메일 중복 확인
        if (headquartersRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        // 8자리 숫자 계정 번호 생성 (중복 확인)
        String accountNumber;
        int attempts = 0;
        do {
            accountNumber = Headquarters.generateNewAccountNumber();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("계정 번호 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
        } while (headquartersRepository.existsByAccountNumber(accountNumber));

        // 회사명 기반 친화적 비밀번호 생성 (최초 설정 시)
        String friendlyPassword = passwordUtil.generateCompanyBasedPassword(request.getCompanyName());
        String encodedPassword = passwordUtil.encodePassword(friendlyPassword);

        // 본사 엔티티 생성
        Headquarters headquarters = Headquarters.builder()
                .accountNumber(accountNumber)
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .department(request.getDepartment())
                .position(request.getPosition())
                .phone(request.getPhone())
                .address(request.getAddress())
                .status(Headquarters.CompanyStatus.ACTIVE)
                .build();

        Headquarters savedHeadquarters = headquartersRepository.save(headquarters);

        log.info("본사 회원가입 완료: {} (계정번호: {})", savedHeadquarters.getEmail(),
                savedHeadquarters.getAccountNumber());

        return HeadquartersDto.SignupResponse.from(savedHeadquarters, friendlyPassword);
    }

    /**
     * 본사 로그인 (이메일 기반)
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

        // JWT 클레임 생성 (8자리 숫자 계정 번호 사용)
        AuthDto.JwtClaims claims = AuthDto.JwtClaims.builder()
                .accountNumber(headquarters.getAccountNumber() != null ? headquarters.getAccountNumber()
                        : headquarters.generateAccountNumber())
                .companyName(headquarters.getCompanyName())
                .userType("HEADQUARTERS")
                .level(null) // 본사는 레벨 없음
                .treePath(null) // 본사는 트리 경로 없음
                .headquartersId(headquarters.getId())
                .userId(headquarters.getId())
                .build();

        // 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(claims);
        String refreshToken = jwtUtil.generateRefreshToken(claims.getAccountNumber());

        log.info("본사 로그인 성공: {} (계정번호: {})", headquarters.getEmail(), claims.getAccountNumber());

        return AuthDto.TokenResponse.of(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                claims.getAccountNumber(),
                headquarters.getCompanyName(),
                "HEADQUARTERS",
                null);
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
                request.getAddress());

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
     * ID로 본사 조회 (내부용)
     */
    public Headquarters findById(Long headquartersId) {
        return headquartersRepository.findById(headquartersId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + headquartersId));
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