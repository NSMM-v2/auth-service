package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.dto.AuthDto;
import com.nsmm.esg.auth_service.dto.PartnerDto;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.entity.Partner;
import com.nsmm.esg.auth_service.repository.PartnerRepository;
import com.nsmm.esg.auth_service.util.JwtUtil;
import com.nsmm.esg.auth_service.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 협력사 서비스 (AWS IAM 방식)
 * 계층적 협력사 계정 생성, 로그인, 관리 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final HeadquartersService headquartersService;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    /**
     * 협력사 계정 생성 (AWS IAM 방식)
     * 본사 또는 상위 협력사에서 하위 협력사 계정을 생성
     */
    @Transactional
    public PartnerDto.CreateResponse createPartner(Long creatorHeadquartersId, Long creatorPartnerId, 
                                                  PartnerDto.CreateRequest request) {
        log.info("협력사 계정 생성 시작: {} (생성자 본사: {}, 생성자 협력사: {})", 
                request.getCompanyName(), creatorHeadquartersId, creatorPartnerId);

        // 이메일 중복 확인
        if (partnerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        // 본사 조회
        Headquarters headquarters = headquartersService.findByAccountNumber(
                String.format("HQ%03d", creatorHeadquartersId));

        // 상위 협력사 조회 (있는 경우)
        Partner parentPartner = null;
        if (request.getParentId() != null) {
            parentPartner = partnerRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상위 협력사입니다: " + request.getParentId()));
            
            // 권한 검증: 생성자가 상위 협력사이거나 본사여야 함
            if (creatorPartnerId != null && !creatorPartnerId.equals(request.getParentId())) {
                throw new IllegalArgumentException("상위 협력사만 하위 협력사를 생성할 수 있습니다.");
            }
        }

        // 레벨 계산
        int level = (parentPartner == null) ? 1 : parentPartner.getLevel() + 1;

        // 계정 번호 생성을 위한 순번 조회
        Integer sequenceNumber = partnerRepository.getNextSequenceNumber(
                headquarters.getId(), request.getParentId());

        // 임시 비밀번호 생성 (AWS IAM 방식)
        String temporaryPassword = passwordUtil.generateTemporaryPassword();
        String encodedPassword = passwordUtil.encodePassword(temporaryPassword);

        // 협력사 엔티티 생성
        Partner partner = Partner.builder()
                .headquarters(headquarters)
                .parent(parentPartner)
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .password(encodedPassword)
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .address(request.getAddress())
                .level(level)
                .status(Partner.PartnerStatus.PENDING)  // 초기 상태는 PENDING
                .passwordChanged(false)
                .temporaryPassword(temporaryPassword)  // 임시 비밀번호 저장 (암호화되지 않은 상태)
                .build();

        // 저장 (ID 생성을 위해)
        Partner savedPartner = partnerRepository.save(partner);

        // 계정 번호 생성 및 설정
        String accountNumber = savedPartner.generateAccountNumber(
                headquarters.generateAccountNumber(), sequenceNumber);
        
        // 트리 경로 생성
        String treePath = savedPartner.generateTreePath();

        // 계정 번호와 트리 경로 설정 후 활성화
        Partner updatedPartner = savedPartner.withAccountNumberAndTreePath(accountNumber, treePath);
        Partner finalPartner = partnerRepository.save(updatedPartner.activate());

        log.info("협력사 계정 생성 완료: {} (계정번호: {}, 레벨: {})", 
                finalPartner.getCompanyName(), finalPartner.getAccountNumber(), finalPartner.getLevel());

        return PartnerDto.CreateResponse.from(finalPartner, temporaryPassword);
    }

    /**
     * 협력사 로그인
     */
    public AuthDto.TokenResponse login(PartnerDto.LoginRequest request) {
        log.info("협력사 로그인 시도: {}", request.getAccountNumber());

        // 활성 상태인 협력사 조회
        Partner partner = partnerRepository.findActiveByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 비활성화된 계정입니다: " + request.getAccountNumber()));

        // 비밀번호 검증
        if (!passwordUtil.matches(request.getPassword(), partner.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 클레임 생성
        AuthDto.JwtClaims claims = AuthDto.JwtClaims.builder()
                .accountNumber(partner.getAccountNumber())
                .companyName(partner.getCompanyName())
                .userType("PARTNER")
                .level(partner.getLevel())
                .treePath(partner.getTreePath())
                .headquartersId(partner.getHeadquarters().getId())
                .userId(partner.getId())
                .build();

        // 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(claims);
        String refreshToken = jwtUtil.generateRefreshToken(partner.getAccountNumber());

        log.info("협력사 로그인 성공: {} (계정번호: {})", partner.getCompanyName(), partner.getAccountNumber());

        return AuthDto.TokenResponse.of(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                partner.getAccountNumber(),
                partner.getCompanyName(),
                "PARTNER",
                partner.getLevel()
        );
    }

    /**
     * 협력사 정보 조회
     */
    public PartnerDto.Response getPartnerInfo(Long partnerId) {
        log.info("협력사 정보 조회: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

        return PartnerDto.Response.from(partner);
    }

    /**
     * 본사의 1차 협력사 목록 조회
     */
    public List<PartnerDto.Response> getTopLevelPartners(Long headquartersId) {
        log.info("1차 협력사 목록 조회: 본사 ID {}", headquartersId);

        List<Partner> partners = partnerRepository.findTopLevelPartnersByHeadquarters(headquartersId);
        return partners.stream()
                .map(PartnerDto.Response::from)
                .toList();
    }

    /**
     * 상위 협력사의 직접 하위 협력사 목록 조회
     */
    public List<PartnerDto.Response> getDirectChildren(Long parentId) {
        log.info("직접 하위 협력사 목록 조회: 상위 협력사 ID {}", parentId);

        List<Partner> children = partnerRepository.findDirectChildrenByParent(parentId);
        return children.stream()
                .map(PartnerDto.Response::from)
                .toList();
    }

    /**
     * 트리 구조 조회 (특정 협력사의 모든 하위 협력사)
     */
    public List<PartnerDto.TreeResponse> getSubTree(Long partnerId) {
        log.info("하위 트리 구조 조회: 협력사 ID {}", partnerId);

        Partner rootPartner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

        List<Partner> subTree = partnerRepository.findSubTreeByTreePath(rootPartner.getTreePath());
        
        // 루트 노드부터 트리 구조 생성
        return subTree.stream()
                .filter(p -> p.getId().equals(partnerId))  // 루트만 반환 (children은 자동으로 포함됨)
                .map(PartnerDto.TreeResponse::from)
                .toList();
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long partnerId, PartnerDto.PasswordChangeRequest request) {
        log.info("협력사 비밀번호 변경: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

        // 현재 비밀번호 확인
        if (!passwordUtil.matches(request.getCurrentPassword(), partner.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호 강도 검증
        if (!passwordUtil.isValidPassword(request.getNewPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 대문자, 소문자, 숫자, 특수문자를 각각 포함해야 합니다.");
        }

        // 비밀번호 변경 (불변성 보장)
        Partner updatedPartner = partner.changePassword(passwordUtil.encodePassword(request.getNewPassword()));
        partnerRepository.save(updatedPartner);
        
        log.info("협력사 비밀번호 변경 완료: {}", partnerId);
    }

    /**
     * 협력사 상태 변경
     */
    @Transactional
    public void changeStatus(Long partnerId, PartnerDto.StatusChangeRequest request) {
        log.info("협력사 상태 변경: {} -> {}", partnerId, request.getStatus());

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

        Partner updatedPartner = partner.changeStatus(request.getStatus());
        partnerRepository.save(updatedPartner);
        
        log.info("협력사 상태 변경 완료: {} -> {} (사유: {})", partnerId, request.getStatus(), request.getReason());
    }

    /**
     * 협력사 정보 수정
     */
    @Transactional
    public PartnerDto.Response updatePartnerInfo(Long partnerId, PartnerDto.UpdateRequest request) {
        log.info("협력사 정보 수정: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

        // 불변성을 보장하는 업데이트
        Partner updatedPartner = partner.updateInfo(
                request.getCompanyName(),
                request.getContactPerson(),
                request.getPhone(),
                request.getAddress()
        );

        Partner savedPartner = partnerRepository.save(updatedPartner);
        
        log.info("협력사 정보 수정 완료: {}", savedPartner.getId());
        
        return PartnerDto.Response.from(savedPartner);
    }

    /**
     * 계정 번호로 협력사 조회 (내부용)
     */
    public Partner findByAccountNumber(String accountNumber) {
        return partnerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + accountNumber));
    }

    /**
     * 비밀번호 변경이 필요한 협력사 목록 조회
     */
    public List<PartnerDto.Response> getPartnersNeedingPasswordChange() {
        List<Partner> partners = partnerRepository.findPartnersNeedingPasswordChange();
        return partners.stream()
                .map(PartnerDto.Response::from)
                .toList();
    }
} 