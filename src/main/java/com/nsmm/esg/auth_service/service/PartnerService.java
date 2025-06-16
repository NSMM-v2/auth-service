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
 * - 협력사 생성 (본사가 1차, 1차가 2차 생성)
 * - 계층적 로그인 (본사계정번호 + 계층적아이디 + 비밀번호)
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
        private final PartnerFactoryService partnerFactoryService;
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

                log.info("협력사 로그인 성공: ID={}, 계층적아이디={}", partner.getId(), partner.getHierarchicalId());

                return partner;
        }

        /**
         * 협력사 생성 (1차 협력사)
         * 본사에서 직접 생성하는 1차 협력사
         */
        @Transactional
        public Partner createFirstLevelPartner(Headquarters headquarters, PartnerCreateRequest request) {
                log.info("1차 협력사 생성 요청: 본사ID={}, 회사명={}", headquarters.getId(), request.getCompanyName());

                // 이메일 중복 검사
                if (partnerRepository.existsByEmail(request.getEmail())) {
                        throw new IllegalArgumentException("이미 등록된 이메일입니다: " + request.getEmail());
                }

                // 협력사 생성 및 저장
                Partner partner = partnerFactoryService.createFirstLevelPartner(headquarters, request);
                Partner savedPartner = partnerRepository.save(partner);

                // 저장 후 실제 ID로 트리 경로 업데이트
                String finalTreePath = partnerTreeService.generateTreePath(savedPartner);
                Partner updatedPartner = Partner.builder()
                                .id(savedPartner.getId())
                                .headquarters(savedPartner.getHeadquarters())
                                .parent(savedPartner.getParent())
                                .children(savedPartner.getChildren())
                                .hqAccountNumber(savedPartner.getHqAccountNumber())
                                .hierarchicalId(savedPartner.getHierarchicalId())
                                .companyName(savedPartner.getCompanyName())
                                .email(savedPartner.getEmail())
                                .password(savedPartner.getPassword())
                                .contactPerson(savedPartner.getContactPerson())
                                .phone(savedPartner.getPhone())
                                .address(savedPartner.getAddress())
                                .level(savedPartner.getLevel())
                                .treePath(finalTreePath)
                                .status(savedPartner.getStatus())
                                .passwordChanged(savedPartner.getPasswordChanged())
                                .createdAt(savedPartner.getCreatedAt())
                                .updatedAt(savedPartner.getUpdatedAt())
                                .build();

                Partner finalPartner = partnerRepository.save(updatedPartner);

                log.info("1차 협력사 생성 완료: ID={}, 계층적아이디={}",
                                finalPartner.getId(), finalPartner.getHierarchicalId());

                return finalPartner;
        }

        /**
         * 협력사 생성 (하위 협력사)
         * 상위 협력사에서 하위 협력사 생성
         */
        @Transactional
        public Partner createSubPartner(Partner parentPartner, PartnerCreateRequest request) {
                log.info("하위 협력사 생성 요청: 상위ID={}, 회사명={}", parentPartner.getId(), request.getCompanyName());

                // 이메일 중복 검사
                if (partnerRepository.existsByEmail(request.getEmail())) {
                        throw new IllegalArgumentException("이미 등록된 이메일입니다: " + request.getEmail());
                }

                // 하위 협력사 생성 및 저장
                Partner partner = partnerFactoryService.createSubPartner(parentPartner, request);
                Partner savedPartner = partnerRepository.save(partner);

                // 저장 후 실제 ID로 트리 경로 업데이트
                String finalTreePath = partnerTreeService.generateTreePath(savedPartner);
                Partner updatedPartner = Partner.builder()
                                .id(savedPartner.getId())
                                .headquarters(savedPartner.getHeadquarters())
                                .parent(savedPartner.getParent())
                                .children(savedPartner.getChildren())
                                .hqAccountNumber(savedPartner.getHqAccountNumber())
                                .hierarchicalId(savedPartner.getHierarchicalId())
                                .companyName(savedPartner.getCompanyName())
                                .email(savedPartner.getEmail())
                                .password(savedPartner.getPassword())
                                .contactPerson(savedPartner.getContactPerson())
                                .phone(savedPartner.getPhone())
                                .address(savedPartner.getAddress())
                                .level(savedPartner.getLevel())
                                .treePath(finalTreePath)
                                .status(savedPartner.getStatus())
                                .passwordChanged(savedPartner.getPasswordChanged())
                                .createdAt(savedPartner.getCreatedAt())
                                .updatedAt(savedPartner.getUpdatedAt())
                                .build();

                Partner finalPartner = partnerRepository.save(updatedPartner);

                log.info("하위 협력사 생성 완료: ID={}, 계층적아이디={}",
                                finalPartner.getId(), finalPartner.getHierarchicalId());

                return finalPartner;
        }

        /**
         * 협력사 정보 조회 (ID)
         */
        public Optional<Partner> findById(Long id) {
                return partnerRepository.findById(id);
        }

        /**
         * 협력사 정보 조회 (이메일)
         */
        public Optional<Partner> findByEmail(String email) {
                return partnerRepository.findByEmail(email);
        }

        /**
         * 협력사 정보 조회 (계층적 식별자)
         */
        public Optional<Partner> findByHierarchicalId(String hqAccountNumber, String hierarchicalId) {
                return partnerRepository.findByHqAccountNumberAndHierarchicalId(hqAccountNumber, hierarchicalId);
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
        public List<Partner> findDirectChildren(Long parentId) {
                return partnerRepository.findDirectChildrenByParentId(parentId);
        }

        /**
         * 협력사 트리 구조 조회 (하위 모든 협력사 포함)
         */
        public List<Partner> findPartnerTree(String treePath) {
                return partnerRepository.findByTreePathStartingWith(treePath);
        }

        /**
         * 초기 비밀번호 변경
         * 협력사 첫 로그인 후 비밀번호 변경
         */
        @Transactional
        public void changeInitialPassword(Long partnerId, String newPassword) {
                log.info("초기 비밀번호 변경 요청: 협력사ID={}", partnerId);

                Partner partner = partnerRepository.findById(partnerId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 협력사입니다: " + partnerId));

                // 새 비밀번호 암호화
                String encodedPassword = passwordUtil.encodePassword(newPassword);

                // 비밀번호 변경 및 변경 플래그 설정
                Partner updatedPartner = Partner.builder()
                                .id(partner.getId())
                                .headquarters(partner.getHeadquarters())
                                .parent(partner.getParent())
                                .children(partner.getChildren())
                                .hqAccountNumber(partner.getHqAccountNumber())
                                .hierarchicalId(partner.getHierarchicalId())
                                .companyName(partner.getCompanyName())
                                .email(partner.getEmail())
                                .password(encodedPassword)
                                .contactPerson(partner.getContactPerson())
                                .phone(partner.getPhone())
                                .address(partner.getAddress())
                                .level(partner.getLevel())
                                .treePath(partner.getTreePath())
                                .status(partner.getStatus())
                                .passwordChanged(true) // 변경 완료 표시
                                .createdAt(partner.getCreatedAt())
                                .updatedAt(partner.getUpdatedAt())
                                .build();

                partnerRepository.save(updatedPartner);
                log.info("초기 비밀번호 변경 완료: 협력사ID={}", partnerId);
        }

        /**
         * 비밀번호 미변경 협력사 목록 조회
         */
        public List<Partner> findUnchangedPasswordPartners(Long headquartersId) {
                return partnerRepository.findUnchangedPasswordPartners(headquartersId);
        }

        /**
         * 본사별 활성 협력사 목록 조회
         */
        public List<Partner> findActivePartners(Long headquartersId) {
                return partnerRepository.findActivePartnersByHeadquarters(headquartersId);
        }

        /**
         * 이메일 중복 확인
         */
        public boolean isEmailDuplicate(String email) {
                return partnerRepository.existsByEmail(email);
        }

        /**
         * 계층적 아이디 중복 확인
         */
        public boolean isHierarchicalIdDuplicate(String hqAccountNumber, String hierarchicalId) {
                return partnerRepository.existsByHqAccountNumberAndHierarchicalId(hqAccountNumber, hierarchicalId);
        }

        /**
         * 본사 계정번호 유효성 검증
         */
        public boolean isValidHqAccountNumber(String hqAccountNumber) {
                return headquartersRepository.existsByHqAccountNumber(hqAccountNumber);
        }
}