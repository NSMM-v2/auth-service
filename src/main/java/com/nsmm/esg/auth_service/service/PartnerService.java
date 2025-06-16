package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.dto.AuthDto;
import com.nsmm.esg.auth_service.dto.PartnerDto;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.entity.Partner;
import com.nsmm.esg.auth_service.repository.HeadquartersRepository;
import com.nsmm.esg.auth_service.repository.PartnerRepository;
import com.nsmm.esg.auth_service.util.JwtUtil;
import com.nsmm.esg.auth_service.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 협력사 서비스 (리팩토링 버전)
 * 
 * 역할: 비즈니스 오케스트레이션
 * - 전문 서비스들을 조합하여 복잡한 비즈니스 로직 수행
 * - 트랜잭션 관리
 * - 권한 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerService {

        private final PartnerRepository partnerRepository;
        private final HeadquartersRepository headquartersRepository;
        private final HeadquartersService headquartersService;
        private final PasswordUtil passwordUtil;
        private final JwtUtil jwtUtil;

        // 새로 추가된 전문 서비스들
        private final PartnerAccountService partnerAccountService;
        private final PartnerTreeService partnerTreeService;
        private final PartnerFactoryService partnerFactoryService;

        /**
         * 협력사 생성 (계층적 아이디 기반)
         */
        @Transactional
        public PartnerDto.CreateResponse createPartner(Long creatorHeadquartersId, Long creatorPartnerId,
                        PartnerDto.CreateRequest request) {
                log.info("협력사 생성 요청: 생성자 본사 ID {}, 생성자 협력사 ID {}, 요청 데이터 {}",
                                creatorHeadquartersId, creatorPartnerId, request);

                // 본사 정보 조회
                Headquarters headquarters = headquartersRepository.findById(creatorHeadquartersId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "존재하지 않는 본사입니다: " + creatorHeadquartersId));

                // 상위 협력사 조회 (선택적)
                Partner parentPartner = null;
                if (request.getParentId() != null) {
                        parentPartner = partnerRepository.findById(request.getParentId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 상위 협력사입니다: " + request.getParentId()));
                }

                // 레벨 계산 (PartnerTreeService 사용)
                int level = partnerTreeService.calculateLevel(parentPartner);

                // 계층적 아이디 생성 (PartnerAccountService 사용)
                String hierarchicalId = partnerAccountService.generateHierarchicalId(
                                request.getContactPerson(), level,
                                parentPartner != null ? parentPartner.getId() : null);

                // 8자리 숫자 계정 번호 생성 (PartnerAccountService 사용)
                String numericAccountNumber = partnerAccountService.generateUniqueNumericAccountNumber();

                // 담당자 이름 기반 임시 비밀번호 생성
                String temporaryPassword = passwordUtil.generateContactBasedPassword(request.getContactPerson());
                String encodedPassword = passwordUtil.encodePassword(temporaryPassword);

                // 협력사 엔티티 생성 (PartnerFactoryService 사용)
                Partner partner = partnerFactoryService.createNewPartner(
                                headquarters, parentPartner, hierarchicalId, numericAccountNumber,
                                request.getCompanyName(), request.getEmail(), encodedPassword,
                                request.getContactPerson(), request.getPhone(), request.getAddress(),
                                level, null, temporaryPassword);

                // 저장 (ID 생성을 위해)
                Partner savedPartner = partnerRepository.save(partner);

                // 트리 경로 생성 및 업데이트 (PartnerTreeService 사용)
                String treePath = partnerTreeService.generateTreePath(savedPartner);
                Partner updatedPartner = partnerFactoryService.withAccountNumberAndTreePath(
                                savedPartner, hierarchicalId, treePath);
                Partner finalPartner = partnerRepository.save(updatedPartner);

                log.info("협력사 생성 완료: ID={}, 계층적아이디={}, 8자리계정={}, 레벨={}",
                                finalPartner.getId(), hierarchicalId, numericAccountNumber, level);

                return PartnerDto.CreateResponse.builder()
                                .partnerId(finalPartner.getId())
                                .hierarchicalId(hierarchicalId)
                                .numericAccountNumber(numericAccountNumber)
                                .temporaryPassword(temporaryPassword)
                                .companyName(request.getCompanyName())
                                .contactPerson(request.getContactPerson())
                                .level(level)
                                .treePath(treePath)
                                .createdAt(finalPartner.getCreatedAt())
                                .message(String.format("협력사가 성공적으로 생성되었습니다. 계층적 아이디: %s", hierarchicalId))
                                .build();
        }

        /**
         * 외부 시스템용 협력사 인증 계정 생성 (호환성 유지)
         */
        @Transactional
        public PartnerDto.AuthAccountCreateResponse createAuthAccount(PartnerDto.AuthAccountCreateRequest request) {
                log.info("외부 시스템용 인증 계정 생성: 파트너 ID {}", request.getPartnerId());

                // 본사 조회
                Headquarters headquarters = headquartersService.findById(request.getHeadquartersId());

                // 상위 협력사 조회 (선택적)
                Partner parentPartner = null;
                if (request.getParentId() != null) {
                        parentPartner = partnerRepository.findById(request.getParentId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 상위 협력사입니다: " + request.getParentId()));
                }

                // 레벨 계산
                int level = partnerTreeService.calculateLevel(parentPartner);

                // 계층적 아이디 생성
                String hierarchicalId = partnerAccountService.generateHierarchicalId(
                                request.getContactPerson(), level,
                                parentPartner != null ? parentPartner.getId() : null);

                // 8자리 숫자 계정 번호 생성
                String numericAccountNumber = partnerAccountService.generateUniqueNumericAccountNumber();

                // 담당자 이름 기반 임시 비밀번호 생성
                String temporaryPassword = passwordUtil.generateContactBasedPassword(request.getContactPerson());
                String encodedPassword = passwordUtil.encodePassword(temporaryPassword);

                // 협력사 엔티티 생성
                Partner partner = partnerFactoryService.createNewPartner(
                                headquarters, parentPartner, hierarchicalId, numericAccountNumber,
                                request.getCompanyName(), request.getEmail(), encodedPassword,
                                request.getContactPerson(), request.getPhone(), request.getAddress(),
                                level, null, temporaryPassword);

                // externalPartnerId 설정
                partner = Partner.builder()
                                .headquarters(partner.getHeadquarters())
                                .parent(partner.getParent())
                                .accountNumber(partner.getAccountNumber())
                                .externalPartnerId(request.getPartnerId())
                                .numericAccountNumber(partner.getNumericAccountNumber())
                                .companyName(partner.getCompanyName())
                                .email(partner.getEmail())
                                .password(partner.getPassword())
                                .contactPerson(partner.getContactPerson())
                                .phone(partner.getPhone())
                                .address(partner.getAddress())
                                .level(partner.getLevel())
                                .treePath(partner.getTreePath())
                                .status(partner.getStatus())
                                .passwordChanged(partner.getPasswordChanged())
                                .temporaryPassword(partner.getTemporaryPassword())
                                .build();

                // 저장
                Partner savedPartner = partnerRepository.save(partner);

                // 트리 경로 생성 및 업데이트
                String treePath = partnerTreeService.generateTreePath(savedPartner);
                Partner updatedPartner = partnerFactoryService.withAccountNumberAndTreePath(
                                savedPartner, hierarchicalId, treePath);
                Partner finalPartner = partnerRepository.save(updatedPartner);

                log.info("계층적 아이디 인증 계정 생성 완료: 파트너ID={}, 8자리계정번호={}, 계층적아이디={}",
                                request.getPartnerId(), numericAccountNumber, finalPartner.getAccountNumber());

                return PartnerDto.AuthAccountCreateResponse.builder()
                                .authId(finalPartner.getId())
                                .partnerId(request.getPartnerId())
                                .accountNumber(numericAccountNumber)
                                .loginId(finalPartner.getAccountNumber()) // 계층적 아이디를 loginId로 사용
                                .temporaryPassword(temporaryPassword)
                                .companyName(request.getCompanyName())
                                .contactPerson(request.getContactPerson())
                                .createdAt(finalPartner.getCreatedAt())
                                .message("계층적 아이디 인증 계정이 생성되었습니다. 계층적 아이디 또는 8자리 계정번호로 로그인하세요.")
                                .build();
        }

        /**
         * 통합 로그인 (계층적 아이디, 8자리 숫자 계정 번호 지원)
         */
        public AuthDto.TokenResponse loginWithAwsStyle(PartnerDto.LoginRequest request) {
                log.info("협력사 로그인 시도: {}", request.getAccountNumber());

                Partner partner = null;
                String accountNumber = request.getAccountNumber().trim();

                // 1. 계층적 아이디로 조회 시도 (p1-kcs01, p2-lyh01 형식)
                if (accountNumber.matches("^p\\d+-[a-z]{2,3}\\d{2}$")) {
                        partner = partnerRepository.findActiveByAccountNumber(accountNumber)
                                        .orElse(null);
                        log.debug("계층적 아이디로 조회: {}", accountNumber);
                }

                // 2. 8자리 숫자 계정 번호로 조회 시도
                if (partner == null && accountNumber.matches("\\d{8}")) {
                        partner = partnerRepository.findActiveByNumericAccountNumber(accountNumber)
                                        .orElse(null);
                        log.debug("8자리 숫자 계정으로 조회: {}", accountNumber);
                }

                // 3. 기존 방식 계정 번호로 조회 시도 (호환성 - HQ001-L1-001 형식)
                if (partner == null) {
                        partner = partnerRepository.findActiveByAccountNumber(accountNumber)
                                        .orElse(null);
                        log.debug("기존 방식 계정번호로 조회: {}", accountNumber);
                }

                if (partner == null) {
                        throw new IllegalArgumentException("존재하지 않거나 비활성화된 계정입니다: " + accountNumber);
                }

                // 비밀번호 검증
                if (!passwordUtil.matches(request.getPassword(), partner.getPassword())) {
                        throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
                }

                // JWT 클레임 생성 (입력된 계정 번호를 우선 사용)
                String displayAccountNumber = determineDisplayAccountNumber(partner, accountNumber);

                AuthDto.JwtClaims claims = AuthDto.JwtClaims.builder()
                                .accountNumber(displayAccountNumber)
                                .companyName(partner.getCompanyName())
                                .userType("PARTNER")
                                .level(partner.getLevel())
                                .treePath(partner.getTreePath())
                                .headquartersId(partner.getHeadquarters().getId())
                                .userId(partner.getId())
                                .build();

                // 토큰 생성
                String accessToken = jwtUtil.generateAccessToken(claims);
                String refreshToken = jwtUtil.generateRefreshToken(displayAccountNumber);

                log.info("협력사 로그인 성공: {} (담당자: {}, 계정번호: {}, 레벨: {})",
                                partner.getCompanyName(), partner.getContactPerson(), displayAccountNumber,
                                partner.getLevel());

                return AuthDto.TokenResponse.of(
                                accessToken,
                                refreshToken,
                                jwtUtil.getAccessTokenExpiration(),
                                displayAccountNumber,
                                partner.getCompanyName(),
                                "PARTNER",
                                partner.getLevel());
        }

        /**
         * 표시할 계정 번호 결정 (로그인에 사용된 형식 우선)
         */
        private String determineDisplayAccountNumber(Partner partner, String inputAccountNumber) {
                // 입력된 계정 번호가 유효하면 그대로 사용
                if (inputAccountNumber.matches("^p\\d+-[a-z]{2,3}\\d{2}$")) {
                        return inputAccountNumber; // 계층적 아이디
                }

                // 기본값으로 계층적 아이디 반환 (새로운 기본 형식)
                if (partner.getAccountNumber() != null
                                && partner.getAccountNumber().matches("^p\\d+-[a-z]{2,3}\\d{2}$")) {
                        return partner.getAccountNumber();
                }

                // 8자리 숫자 계정이 있으면 반환
                if (partner.getNumericAccountNumber() != null) {
                        return partner.getNumericAccountNumber();
                }

                // 최종적으로 저장된 계정 번호 반환
                return partner.getAccountNumber();
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
         * 협력사 하위 트리 구조 조회
         */
        public List<PartnerDto.TreeResponse> getSubTree(Long partnerId) {
                log.info("하위 트리 구조 조회: 협력사 ID {}", partnerId);

                Partner partner = partnerRepository.findById(partnerId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

                List<Partner> subTree = partnerRepository.findSubTreeByTreePath(partner.getTreePath());
                return subTree.stream()
                                .map(p -> PartnerDto.TreeResponse.builder()
                                                .partnerId(p.getId())
                                                .accountNumber(p.getAccountNumber())
                                                .companyName(p.getCompanyName())
                                                .contactPerson(p.getContactPerson())
                                                .level(p.getLevel())
                                                .treePath(p.getTreePath())
                                                .parentId(p.getParent() != null ? p.getParent().getId() : null)
                                                .status(p.getStatus().toString())
                                                .build())
                                .toList();
        }

        /**
         * 비밀번호 변경 (PartnerFactoryService 사용)
         */
        @Transactional
        public void changePassword(Long partnerId, PartnerDto.PasswordChangeRequest request) {
                log.info("비밀번호 변경: 협력사 ID {}", partnerId);

                Partner partner = partnerRepository.findById(partnerId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

                // 현재 비밀번호 확인
                if (!passwordUtil.matches(request.getCurrentPassword(), partner.getPassword())) {
                        throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
                }

                // 새 비밀번호 암호화
                String encodedNewPassword = passwordUtil.encodePassword(request.getNewPassword());

                // PartnerFactoryService를 통한 비밀번호 변경
                Partner updatedPartner = partnerFactoryService.changePassword(partner, encodedNewPassword);
                partnerRepository.save(updatedPartner);

                log.info("비밀번호 변경 완료: 협력사 ID {}", partnerId);
        }

        /**
         * 협력사 상태 변경 (PartnerFactoryService 사용)
         */
        @Transactional
        public void changeStatus(Long partnerId, PartnerDto.StatusChangeRequest request) {
                log.info("협력사 상태 변경: 협력사 ID {}, 새 상태 {}", partnerId, request.getStatus());

                Partner partner = partnerRepository.findById(partnerId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

                // PartnerFactoryService를 통한 상태 변경
                Partner updatedPartner = partnerFactoryService.changeStatus(partner, request.getStatus());
                partnerRepository.save(updatedPartner);

                log.info("협력사 상태 변경 완료: 협력사 ID {}, 상태 {}", partnerId, request.getStatus());
        }

        /**
         * 협력사 정보 업데이트 (PartnerFactoryService 사용)
         */
        @Transactional
        public PartnerDto.Response updatePartnerInfo(Long partnerId, PartnerDto.UpdateRequest request) {
                log.info("협력사 정보 업데이트: 협력사 ID {}", partnerId);

                Partner partner = partnerRepository.findById(partnerId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

                // PartnerFactoryService를 통한 정보 업데이트
                Partner updatedPartner = partnerFactoryService.updatePartnerInfo(
                                partner, request.getCompanyName(), request.getContactPerson(),
                                request.getPhone(), request.getAddress());

                Partner savedPartner = partnerRepository.save(updatedPartner);

                log.info("협력사 정보 업데이트 완료: 협력사 ID {}", partnerId);
                return PartnerDto.Response.from(savedPartner);
        }

        /**
         * 계정 번호로 협력사 조회 (내부 사용)
         */
        public Partner findByAccountNumber(String accountNumber) {
                return partnerRepository.findByAccountNumber(accountNumber)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정 번호입니다: " + accountNumber));
        }

        /**
         * 비밀번호 변경이 필요한 협력사 목록 조회
         */
        public List<PartnerDto.Response> getPartnersNeedingPasswordChange() {
                log.info("비밀번호 변경이 필요한 협력사 목록 조회");

                List<Partner> partners = partnerRepository.findPartnersNeedingPasswordChange();
                return partners.stream()
                                .map(PartnerDto.Response::from)
                                .toList();
        }
}