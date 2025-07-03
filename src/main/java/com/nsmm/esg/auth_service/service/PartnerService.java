package com.nsmm.esg.auth_service.service;

import com.nsmm.esg.auth_service.dto.partner.PartnerCreateRequest;
import com.nsmm.esg.auth_service.entity.Headquarters;
import com.nsmm.esg.auth_service.entity.Partner;
import com.nsmm.esg.auth_service.repository.HeadquartersRepository;
import com.nsmm.esg.auth_service.repository.PartnerRepository;
import com.nsmm.esg.auth_service.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 협력사 비즈니스 로직 서비스
 * 
 * 주요 기능:
 * - UUID 기반 협력사 생성 (프론트엔드 요구사항)
 * - 계층적 로그인 (본사계정번호 + 계층적아이디 + 비밀번호)
 * - 권한 제어 (본인 + 직속 하위 1단계)
 * - 트리 구조 관리
 * - 초기 비밀번호 변경 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PartnerService {

        private final PartnerRepository partnerRepository;
        private final HeadquartersRepository headquartersRepository;
        private final PasswordUtil passwordUtil;

        // 전문 서비스들
        private final PartnerAccountService partnerAccountService;
        private final PartnerTreeService partnerTreeService;

        /**
         * 협력사 로그인 (본사계정번호 + 계층적아이디 + 비밀번호)
         */
        public Partner login(String hqAccountNumber, String hierarchicalId, String password) {
                log.info("협력사 로그인 요청: 본사계정번호={}, 계층적아이디={}", hqAccountNumber, hierarchicalId);

                // 본사 계정번호 존재 여부 검증
                if (!headquartersRepository.existsByHqAccountNumber(hqAccountNumber)) {
                        throw new BadCredentialsException("존재하지 않는 본사 계정번호입니다: " + hqAccountNumber);
                }

                // 협력사 조회
                Partner partner = partnerRepository
                                .findByHqAccountNumberAndHierarchicalId(hqAccountNumber, hierarchicalId)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "존재하지 않는 협력사입니다: " + hqAccountNumber + "-" + hierarchicalId));

                // 계정 상태 확인
                if (!Partner.PartnerStatus.ACTIVE.equals(partner.getStatus())) {
                        throw new BadCredentialsException("비활성화된 계정입니다.");
                }

                // 비밀번호 검증
                if (!passwordUtil.matches(password, partner.getPassword())) {
                        throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
                }

                log.info("협력사 로그인 성공: ID={}, 계층적아이디={}", partner.getPartnerId(), partner.getHierarchicalId());

                return partner;
        }

        /**
         * UUID 기반 협력사 생성 (DART API 기반)
         * DART API에서 제공받은 회사 정보로 협력사를 생성합니다.
         */
        @Transactional
        public Partner createPartnerByUuid(Long creatorHeadquartersId, PartnerCreateRequest request) {
                log.info("DART API 기반 협력사 생성 시작: 생성자본사ID={}, UUID={}, 회사명={}, 상위UUID={}",
                                creatorHeadquartersId, request.getUuid(), request.getCompanyName(),
                                request.getParentUuid());

                // UUID 중복 확인
                if (partnerRepository.existsByUuid(request.getUuid())) {
                        throw new IllegalArgumentException("이미 존재하는 UUID입니다: " + request.getUuid());
                }

                // 본사 조회
                Headquarters headquarters = headquartersRepository.findById(creatorHeadquartersId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "존재하지 않는 본사입니다: " + creatorHeadquartersId));

                Partner savedPartner;

                if (request.getParentUuid() == null || request.getParentUuid().equals(headquarters.getUuid())) {
                        // 1차 협력사 생성
                        savedPartner = createFirstLevelPartner(headquarters, request);
                } else {
                        // 하위 협력사 생성
                        Partner parentPartner = partnerRepository.findByUuid(request.getParentUuid())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "존재하지 않는 상위 협력사 UUID입니다: " + request.getParentUuid()));

                        savedPartner = createSubPartner(parentPartner, request);
                }

                log.info("DART API 기반 협력사 생성 완료: ID={}, UUID={}, 계층적아이디={}",
                                savedPartner.getPartnerId(), savedPartner.getUuid(), savedPartner.getHierarchicalId());

                return savedPartner;
        }

        /**
         * 1차 협력사 생성 (내부 메서드)
         */
        private Partner createFirstLevelPartner(Headquarters headquarters, PartnerCreateRequest request) {
                // 계층적 ID 생성 (L1-001, L1-002...)
                String hierarchicalId = partnerAccountService.generateHierarchicalId(headquarters.getHeadquartersId(),
                                1, null);

                // 트리 경로 생성
                String treePath = partnerTreeService.generateTreePath(headquarters.getHqAccountNumber(), hierarchicalId,
                                null);

                // 초기 비밀번호 (계층적 ID와 동일)
                String initialPassword = passwordUtil.encodePassword(hierarchicalId);

                Partner partner = Partner.builder()
                                .uuid(request.getUuid())
                                .headquarters(headquarters)
                                .parentPartner(null) // 1차 협력사는 상위가 없음
                                .hqAccountNumber(headquarters.getHqAccountNumber())
                                .hierarchicalId(hierarchicalId)
                                .companyName(request.getCompanyName())
                                .password(initialPassword)
                                .level(1)
                                .treePath(treePath)
                                .status(Partner.PartnerStatus.ACTIVE)
                                .passwordChanged(false)
                                .build();

                return partnerRepository.save(partner);
        }

        /**
         * 하위 협력사 생성 (내부 메서드)
         */
        private Partner createSubPartner(Partner parentPartner, PartnerCreateRequest request) {
                // 계층적 ID 생성
                String hierarchicalId = partnerAccountService.generateHierarchicalId(
                                parentPartner.getHeadquarters().getHeadquartersId(),
                                parentPartner.getLevel() + 1,
                                parentPartner.getPartnerId());

                // 트리 경로 생성
                String treePath = partnerTreeService.generateTreePath(
                                parentPartner.getHqAccountNumber(),
                                hierarchicalId,
                                parentPartner.getTreePath());

                // 초기 비밀번호 (계층적 ID와 동일)
                String initialPassword = passwordUtil.encodePassword(hierarchicalId);

                Partner partner = Partner.builder()
                                .uuid(request.getUuid())
                                .headquarters(parentPartner.getHeadquarters())
                                .parentPartner(parentPartner)
                                .hqAccountNumber(parentPartner.getHqAccountNumber())
                                .hierarchicalId(hierarchicalId)
                                .companyName(request.getCompanyName())
                                .password(initialPassword)
                                .level(parentPartner.getLevel() + 1)
                                .treePath(treePath)
                                .status(Partner.PartnerStatus.ACTIVE)
                                .passwordChanged(false)
                                .build();

                return partnerRepository.save(partner);
        }

        /**
         * 권한 제어: 접근 가능한 협력사 목록 조회 (본인 + 직속 하위 1단계)
         * 본사: 모든 협력사, 협력사: 본인 + 직속 하위만
         */
        public List<Partner> findAccessiblePartners(String userType, Long userId, String treePath, Integer level) {
                if ("HEADQUARTERS".equals(userType)) {
                        // 본사는 모든 협력사 접근 가능
                        Headquarters headquarters = headquartersRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 본사입니다: " + userId));
                        return partnerRepository.findAllPartnersByHeadquarters(headquarters.getHeadquartersId());
                } else {
                        // 협력사는 본인 + 직속 하위 1단계만
                        return findAccessiblePartnersForPartner(treePath, level);
                }
        }

        /**
         * 협력사용 접근 가능한 파트너 조회 (본인 + 직속 하위 1단계)
         */
        private List<Partner> findAccessiblePartnersForPartner(String currentTreePath, Integer currentLevel) {
                Integer directChildLevel = currentLevel + 1;
                Integer expectedSlashCount = currentTreePath.length() - currentTreePath.replace("/", "").length() + 1;

                return partnerRepository.findAccessiblePartners(currentTreePath, directChildLevel, expectedSlashCount);
        }

        /**
         * 협력사 정보 조회 (ID)
         */
        public Optional<Partner> findById(Long id) {
                return partnerRepository.findById(id);
        }

        /**
         * 현재 로그인한 협력사 사용자 정보 조회
         * JWT 토큰에서 추출한 협력사 ID로 협력사 정보를 조회합니다.
         */
        public Partner getCurrentUser(Long currentPartnerId) {
                log.info("현재 협력사 사용자 정보 조회: ID={}", currentPartnerId);

                Partner partner = partnerRepository.findById(currentPartnerId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + currentPartnerId));

                // 계정 상태 확인
                if (!Partner.PartnerStatus.ACTIVE.equals(partner.getStatus())) {
                        throw new IllegalStateException("비활성화된 계정입니다.");
                }

                log.info("현재 협력사 사용자 정보 조회 완료: 계층적아이디={}, 회사명={}",
                                partner.getHierarchicalId(), partner.getCompanyName());

                return partner;
        }

        /**
         * 협력사 정보 조회 (UUID)
         */
        public Optional<Partner> findByUuid(String uuid) {
                return partnerRepository.findByUuid(uuid);
        }

        /**
         * 협력사 로그인 (본사계정번호 + 협력사아이디 + 비밀번호)
         * 프론트엔드 요구사항에 맞는 새로운 로그인 방식
         */
        public Partner loginByHqAndPartnerCode(String hqAccountNumber, String partnerCode, String password) {
                log.info("협력사 로그인 시도: 본사계정번호={}, 협력사아이디={}", hqAccountNumber, partnerCode);

                // hqAccountNumber와 hierarchicalId(partnerCode)로 협력사 조회
                Partner partner = partnerRepository.findByHqAccountNumberAndHierarchicalId(
                                hqAccountNumber, partnerCode)
                                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 협력사 계정입니다."));

                // 계정 상태 확인
                if (!Partner.PartnerStatus.ACTIVE.equals(partner.getStatus())) {
                        throw new BadCredentialsException("비활성화된 계정입니다.");
                }

                // 비밀번호 검증
                if (!passwordUtil.matches(password, partner.getPassword())) {
                        throw new BadCredentialsException("비밀번호가 올바르지 않습니다.");
                }

                log.info("협력사 로그인 성공: 계정번호={}, 회사명={}, 비밀번호변경여부={}",
                                partner.getFullAccountNumber(), partner.getCompanyName(), partner.getPasswordChanged());

                return partner;
        }

        /**
         * 본사별 1차 협력사 목록 조회
         */
        public List<Partner> findFirstLevelPartners(Long headquartersId) {
                return partnerRepository.findFirstLevelPartnersByHeadquarters(headquartersId);
        }

        /**
         * 특정 협력사의 직접 하위 협력사 목록 조회
         */
        public List<Partner> findDirectChildren(Long parentPartnerId) {
                return partnerRepository.findDirectChildrenByParentId(parentPartnerId);
        }

        /**
         * 초기 비밀번호 변경
         */
        @Transactional
        public void changeInitialPassword(Long partnerId, String newPassword) {
                log.info("초기 비밀번호 변경 요청: 협력사ID={}", partnerId);

                Partner partner = partnerRepository.findById(partnerId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

                // 새 비밀번호 암호화
                String encodedPassword = passwordUtil.encodePassword(newPassword);

                // 비밀번호 변경 (불변성 보장)
                Partner updatedPartner = partner.changePassword(encodedPassword);
                partnerRepository.save(updatedPartner);

                log.info("초기 비밀번호 변경 완료: 협력사ID={}", partnerId);
        }

        /**
         * 전체 계정번호로 협력사 조회
         */
        public Optional<Partner> findByFullAccountNumber(String fullAccountNumber) {
                // 계정번호 파싱 (HQ001-L1-001 -> HQ001, L1-001)
                String[] parts = fullAccountNumber.split("-", 2);
                if (parts.length != 2) {
                        throw new IllegalArgumentException("올바르지 않은 계정번호 형식입니다: " + fullAccountNumber);
                }

                String hqAccountNumber = parts[0];
                String hierarchicalId = parts[1];

                return partnerRepository.findByHqAccountNumberAndHierarchicalId(hqAccountNumber, hierarchicalId);
        }

        /**
         * 비밀번호 미변경 협력사 목록 조회
         */
        public List<Partner> findUnchangedPasswordPartners(Long headquartersId) {
                return partnerRepository.findUnchangedPasswordPartners(headquartersId);
        }

        /**
         * UUID 중복 확인
         */
        public boolean isUuidDuplicate(String uuid) {
                return partnerRepository.existsByUuid(uuid);
        }

}
