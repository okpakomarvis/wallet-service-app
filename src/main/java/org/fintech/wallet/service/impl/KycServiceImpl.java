package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.KycVerification;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.dto.event.KycEvent;
import org.fintech.wallet.dto.request.AdminKycApprovalRequest;
import org.fintech.wallet.dto.request.KycSubmissionRequest;
import org.fintech.wallet.dto.response.KycResponse;
import org.fintech.wallet.dto.response.KycVerificationResponse;
import org.fintech.wallet.exception.KycRequiredException;
import org.fintech.wallet.exception.UserNotFoundException;
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
    public KycResponse submitKyc(UUID userId,
                                 MultipartFile idDocument,
                                 MultipartFile proofOfAddress,
                                 MultipartFile selfie)  {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // fullname from signup
        String fullName = user.getFirstName() + " " + user.getLastName();

        //  Determine next KYC level
        KycLevel level = getNextKycLevelForUser(user);
        if (level == null) {
            throw new KycRequiredException("User has already completed full KYC verification");
        }

        if (kycRepository.existsByUserIdAndLevel(userId, level)) {
            throw new KycRequiredException("KYC already submitted for this level");
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
                    .level(level)
                    .status(KycStatus.PENDING)
                    .fullName(fullName)
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
    public KycResponse approveKyc(UUID kycId,
                                  UUID adminId,
                                  AdminKycApprovalRequest request) {

        KycVerification kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new KycRequiredException("KYC not found"));

        User user = kyc.getUser();

        // 🔥 Determine user's current level
        KycLevel currentLevel = user.getKycLevel() == null
                ? KycLevel.NONE
                : user.getKycLevel();

        // 🔥 Determine expected next level
        KycLevel expectedNextLevel = getNextLevel(currentLevel);

        if (expectedNextLevel == null) {
            throw new KycRequiredException("User has already completed full KYC verification");
        }

        // 🔥 Ensure admin is approving the correct next level
        if (kyc.getLevel() != expectedNextLevel) {
            throw new KycRequiredException(
                    "Invalid tier approval. Expected: " + expectedNextLevel);
        }

        if (kyc.getStatus() == KycStatus.VERIFIED) {
            return mapToResponse(kyc);
        }

        //  Approve KYC
        kyc.setStatus(KycStatus.VERIFIED);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(LocalDateTime.now());

        // 🔥 Update only non-empty admin fields
        if (request.getIdType() != null) {
            kyc.setIdType(request.getIdType());
        }

        if (request.getNationality() != null && !request.getNationality().isBlank()) {
            kyc.setNationality(request.getNationality());
        }

        if (request.getIdNumber() != null && !request.getIdNumber().isBlank()) {
            kyc.setIdNumber(request.getIdNumber());
        }

        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            kyc.setAddress(request.getAddress());
        }

        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            kyc.setCountry(request.getCountry());
        }

        if (request.getDateOfBirth() != null) {
            kyc.setDateOfBirth(request.getDateOfBirth());
        }

        kycRepository.save(kyc);

        //  Move user to the next level
        user.setKycLevel(expectedNextLevel);

        // Only set VERIFIED if max tier reached
        if (getNextLevel(expectedNextLevel) == null) {
            user.setKycStatus(KycStatus.VERIFIED);
        }

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
    public Page<KycVerificationResponse> getPendingKyc(Pageable pageable) {
        return kycRepository.findByStatus(KycStatus.PENDING, pageable)
                .map(this::mapToResponseAdmin);
    }
    private KycLevel getNextKycLevelForUser(User user) {
        KycLevel current = user.getKycLevel() == null ? KycLevel.NONE : user.getKycLevel();

        return switch (current) {
            case NONE -> KycLevel.TIER_1;
            case TIER_1 -> KycLevel.TIER_2;
            case TIER_2 -> KycLevel.TIER_3;
            case TIER_3 -> null; // max KYC reached
        };
    }
    private KycLevel getNextLevel(KycLevel current) {
        return switch (current) {
            case NONE -> KycLevel.TIER_1;
            case TIER_1 -> KycLevel.TIER_2;
            case TIER_2 -> KycLevel.TIER_3;
            case TIER_3 -> null; // max reached
        };
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
    private KycVerificationResponse mapToResponseAdmin(KycVerification kyc) {

        return KycVerificationResponse.builder()
                .id(kyc.getId())

                // User (flattened)
                .userId(kyc.getUser().getId())
                .userEmail(kyc.getUser().getEmail())

                // KYC Status
                .level(kyc.getLevel() != null ? kyc.getLevel().name() : null)
                .status(kyc.getStatus() != null ? kyc.getStatus().name() : null)

                // Personal Info
                .fullName(kyc.getFullName())
                .idType(kyc.getIdType())
                .idNumber(kyc.getIdNumber())
                .dateOfBirth(kyc.getDateOfBirth())
                .nationality(kyc.getNationality())

                // Address
                .address(kyc.getAddress())
                .city(kyc.getCity())
                .state(kyc.getState())
                .postalCode(kyc.getPostalCode())
                .country(kyc.getCountry())

                // Documents
                .idDocumentUrl(kyc.getIdDocumentUrl())
                .proofOfAddressUrl(kyc.getProofOfAddressUrl())
                .selfieUrl(kyc.getSelfieUrl())

                // Verification
                .verificationProvider(kyc.getVerificationProvider())
                .externalVerificationId(kyc.getExternalVerificationId())
                .verifiedAt(kyc.getVerifiedAt())
                .rejectionReason(kyc.getRejectionReason())
                .reviewedBy(kyc.getReviewedBy())
                .reviewedAt(kyc.getReviewedAt())

                // Audit
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())

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
