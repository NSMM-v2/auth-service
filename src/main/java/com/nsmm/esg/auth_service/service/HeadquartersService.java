package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.dto.headquarters.HeadquartersSignupRequest;
import com.nsmm.esg.auth_service.dto.headquarters.HeadquartersLoginRequest;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.repository.HeadquartersRepository;
import com.nsmm.esg.auth_service.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 본사 비즈니스 로직 서비스
 * 
 * 주요 기능:
 * - 본사 회원가입 (동적 계정번호 생성, UUID 자동 생성)
 * - 이메일 기반 로그인
 * - UUID 기반 조회
 * - 본사 정보 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class HeadquartersService {

    private final HeadquartersRepository headquartersRepository;
    private final PasswordUtil passwordUtil;
    private final HeadquartersAccountService headquartersAccountService;

    /**
     * 본사 회원가입
     * 계정번호 자동 생성 (HQ + YYYYMMDD + 순번), UUID 자동 생성, 이메일 중복 검사
     */
    @Transactional
    public Headquarters register(HeadquartersSignupRequest registrationDto) {
        log.info("본사 회원가입 요청: 이메일={}, 회사명={}",
                registrationDto.getEmail(), registrationDto.getCompanyName());

        // 이메일 중복 검사
        if (headquartersRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다: " + registrationDto.getEmail());
        }

        // UUID 생성 (외부 API 연동용)
        String uuid = generateUniqueUuid();
        log.info("생성된 본사 UUID: {}", uuid);

        // 새로운 본사 계정번호 생성
        String hqAccountNumber = headquartersAccountService.generateAccountNumber();
        log.info("생성된 본사 계정번호: {}", hqAccountNumber);

        // 비밀번호 암호화
        String encodedPassword = passwordUtil.encodePassword(registrationDto.getPassword());

        // 본사 엔티티 생성
        Headquarters headquarters = Headquarters.builder()
                .uuid(uuid)
                .hqAccountNumber(hqAccountNumber)
                .companyName(registrationDto.getCompanyName())
                .email(registrationDto.getEmail())
                .password(encodedPassword)
                .name(registrationDto.getName())
                .department(registrationDto.getDepartment())
                .position(registrationDto.getPosition())
                .phone(registrationDto.getPhone())
                .address(registrationDto.getAddress())
                .status(Headquarters.CompanyStatus.ACTIVE)
                .build();

        Headquarters savedHeadquarters = headquartersRepository.save(headquarters);
        log.info("본사 회원가입 완료: ID={}, UUID={}, 계정번호={}",
                savedHeadquarters.getHeadquartersId(), savedHeadquarters.getUuid(),
                savedHeadquarters.getHqAccountNumber());

        return savedHeadquarters;
    }

    /**
     * 본사 로그인 (이메일 + 비밀번호)
     */
    public Headquarters login(HeadquartersLoginRequest loginDto) {
        log.info("본사 로그인 요청: 이메일={}", loginDto.getEmail());

        // 이메일로 본사 조회
        Headquarters headquarters = headquartersRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일입니다: " + loginDto.getEmail()));

        // 계정 상태 확인
        if (!headquarters.isActive()) {
            throw new BadCredentialsException("비활성화된 계정입니다.");
        }

        // 비밀번호 검증
        if (!passwordUtil.matches(loginDto.getPassword(), headquarters.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        log.info("본사 로그인 성공: ID={}, 계정번호={}",
                headquarters.getHeadquartersId(), headquarters.getHqAccountNumber());

        return headquarters;
    }

    /**
     * 본사 정보 조회 (ID)
     */
    public Optional<Headquarters> findById(Long id) {
        return headquartersRepository.findById(id);
    }

    /**
     * 본사 정보 조회 (UUID)
     */
    public Optional<Headquarters> findByUuid(String uuid) {
        log.info("UUID로 본사 조회: {}", uuid);
        return headquartersRepository.findByUuid(uuid);
    }

    /**
     * 본사 정보 수정
     */
    @Transactional
    public Headquarters updateHeadquarters(Long id, String companyName, String name,
            String department, String position, String phone, String address) {
        log.info("본사 정보 수정 요청: ID={}", id);

        Headquarters headquarters = headquartersRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + id));

        // 정보 업데이트 (불변성 보장)
        Headquarters updatedHeadquarters = headquarters.updateInfo(
                companyName, name, department, position, phone, address);

        Headquarters savedHeadquarters = headquartersRepository.save(updatedHeadquarters);
        log.info("본사 정보 수정 완료: ID={}", savedHeadquarters.getHeadquartersId());

        return savedHeadquarters;
    }

    /**
     * 본사 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        log.info("본사 비밀번호 변경 요청: ID={}", id);

        Headquarters headquarters = headquartersRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + id));

        // 기존 비밀번호 확인
        if (!passwordUtil.matches(oldPassword, headquarters.getPassword())) {
            throw new BadCredentialsException("기존 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 암호화
        String encodedNewPassword = passwordUtil.encodePassword(newPassword);

        // 비밀번호 변경 (불변성 보장)
        Headquarters updatedHeadquarters = headquarters.changePassword(encodedNewPassword);
        headquartersRepository.save(updatedHeadquarters);

        log.info("본사 비밀번호 변경 완료: ID={}", id);
    }

    /**
     * 이메일 중복 확인
     */
    public boolean isEmailDuplicate(String email) {
        return headquartersRepository.existsByEmail(email);
    }

    /**
     * UUID 중복 확인
     */
    public boolean isUuidDuplicate(String uuid) {
        return headquartersRepository.existsByUuid(uuid);
    }

    /**
     * 다음 생성될 본사 계정번호 미리 확인
     */
    public String getNextAccountNumber() {
        return headquartersAccountService.getNextAvailableAccountNumber();
    }

    /**
     * 본사 계정번호 유효성 검증
     */
    public boolean isValidAccountNumber(String accountNumber) {
        return headquartersAccountService.isValidAccountNumber(accountNumber);
    }

    /**
     * 중복되지 않는 UUID 생성 (접두사 없는 순수 UUID)
     */
    private String generateUniqueUuid() {
        String uuid;
        int attempts = 0;
        do {
            uuid = UUID.randomUUID().toString();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("UUID 생성에 실패했습니다. 최대 시도 횟수를 초과했습니다.");
            }
        } while (headquartersRepository.existsByUuid(uuid));

        return uuid;
    }
}