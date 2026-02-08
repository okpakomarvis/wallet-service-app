package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.KycVerification;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.dto.event.KycEvent;
import org.fintech.wallet.dto.request.KycSubmissionRequest;
import org.fintech.wallet.dto.response.KycResponse;
import org.fintech.wallet.exception.KycRequiredException;
import org.fintech.wallet.kafka.KafkaProducerService;
import org.fintech.wallet.repository.KycRepository;
import org.fintech.wallet.repository.UserRepository;
import org.fintech.wallet.service.FileStorageService;
import org.fintech.wallet.service.KycService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl implements KycService {

    private final KycRepository kycRepository;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public KycResponse submitKyc(
            UUID userId,
            KycSubmissionRequest request,
            MultipartFile idDocument,
            MultipartFile proofOfAddress,
            MultipartFile selfie
    ) {
        log.info("KYC submission: user={}, level={}", userId, request.getLevel());

        if (request.getLevel() == null || request.getLevel() == KycLevel.NONE) {
            throw new IllegalArgumentException("Invalid KYC level");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // One KYC per level per user
        if (kycRepository.existsByUserIdAndLevel(userId, request.getLevel())) {
            throw new IllegalStateException("KYC already submitted for this level");
        }

        // Upload documents
        FileStorageService.UploadResult idUp =
                fileStorageService.uploadKycFile(userId, "id_document", idDocument);
        FileStorageService.UploadResult addrUp =
                fileStorageService.uploadKycFile(userId, "proof_of_address", proofOfAddress);
        FileStorageService.UploadResult selfieUp =
                fileStorageService.uploadKycFile(userId, "selfie", selfie);

        try {
            KycVerification kyc = KycVerification.builder()
                    .user(user)
                    .level(request.getLevel())
                    .status(KycStatus.PENDING)
                    .fullName(request.getFullName())
                    .idType(request.getIdType())
                    .idNumber(request.getIdNumber())
                    .dateOfBirth(request.getDateOfBirth())
                    .nationality(request.getNationality())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .state(request.getState())
                    .postalCode(request.getPostalCode())
                    .country(request.getCountry())

                    // URLs
                    .idDocumentUrl(idUp.secureUrl())
                    .proofOfAddressUrl(addrUp.secureUrl())
                    .selfieUrl(selfieUp.secureUrl())

                    // Cloudinary metadata
                    .idDocumentPublicId(idUp.publicId())
                    .idDocumentResourceType(idUp.resourceType())
                    .proofOfAddressPublicId(addrUp.publicId())
                    .proofOfAddressResourceType(addrUp.resourceType())
                    .selfiePublicId(selfieUp.publicId())
                    .selfieResourceType(selfieUp.resourceType())
                    .build();

            kyc = kycRepository.save(kyc);

            publishKycEvent(kyc, "SUBMITTED", null);
            return mapToResponse(kyc);

        } catch (Exception e) {
            // cleanup uploads
            fileStorageService.deleteByPublicId(idUp.publicId(), idUp.resourceType());
            fileStorageService.deleteByPublicId(addrUp.publicId(), addrUp.resourceType());
            fileStorageService.deleteByPublicId(selfieUp.publicId(), selfieUp.resourceType());
            throw e;
        }
    }
    @Override
    @Transactional
    public KycResponse approveKyc(UUID kycId, UUID adminId) {

        KycVerification kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        if (kyc.getStatus() == KycStatus.VERIFIED) {
            return mapToResponse(kyc);
        }

        kyc.setStatus(KycStatus.VERIFIED);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(LocalDateTime.now());
        kycRepository.save(kyc);

        // Update user's highest verified tier
        User user = kyc.getUser();
        KycLevel highestLevel = kycRepository
                .findHighestVerifiedLevel(user.getId())
                .orElse(KycLevel.NONE);

        user.setKycLevel(highestLevel);
        user.setKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        publishKycEvent(kyc, "APPROVED", adminId);
        return mapToResponse(kyc);
    }
    @Override
    @Transactional
    public KycResponse rejectKyc(UUID kycId, UUID adminId, String reason) {

        KycVerification kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        kyc.setStatus(KycStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(LocalDateTime.now());
        kycRepository.save(kyc);

        // DO NOT downgrade user — keep highest verified tier
        publishKycEvent(kyc, "REJECTED", adminId);
        return mapToResponse(kyc);
    }


    @Transactional(readOnly = true)
    public KycResponse getUserKyc(UUID userId) {
        KycVerification kyc = kycRepository.findByUserId(userId)
                .orElseThrow(() -> new KycRequiredException("KYC verification not found"));
        return mapToResponse(kyc);
    }

    @Transactional(readOnly = true)
    public Page<KycResponse> getPendingKyc(Pageable pageable) {
        return kycRepository.findByStatus(KycStatus.PENDING, pageable)
                .map(this::mapToResponse);
    }

    private void publishKycEvent(KycVerification kyc, String action, UUID adminId) {
        try {
            KycEvent event = KycEvent.builder()
                    .kycId(kyc.getId())
                    .userId(kyc.getUser().getId())
                    .status(kyc.getStatus().name())
                    .level(kyc.getLevel() != null ? kyc.getLevel().name() : null)
                    .action(action)
                    .reviewedBy(adminId)
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaProducerService.publishKycEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish KYC event: kycId={}, action={}", kyc.getId(), action, e);
        }
    }

    private KycResponse mapToResponse(KycVerification kyc) {
        return KycResponse.builder()
                .id(kyc.getId())
                .userId(kyc.getUser().getId())
                .level(kyc.getLevel())
                .status(kyc.getStatus())
                .fullName(kyc.getFullName())
                .idType(kyc.getIdType())
                .dateOfBirth(kyc.getDateOfBirth())
                .nationality(kyc.getNationality())
                .verifiedAt(kyc.getVerifiedAt())
                .rejectionReason(kyc.getRejectionReason())
                .createdAt(kyc.getCreatedAt())
                .build();
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { task.run(); }
            });
        } else {
            task.run();
        }
    }
}
