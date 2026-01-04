package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.KycVerification;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.dto.request.KycSubmissionRequest;
import org.fintech.wallet.dto.response.KycResponse;
import org.fintech.wallet.repository.KycRepository;
import org.fintech.wallet.repository.UserRepository;
import org.fintech.wallet.service.KycService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl implements KycService {

    private final KycRepository kycRepository;
    private final UserRepository userRepository;
    // private final FileStorageService fileStorageService; // cloud storage service
    @Override
    @Transactional
    public KycResponse submitKyc(UUID userId, KycSubmissionRequest request,
                                 MultipartFile idDocument,
                                 MultipartFile proofOfAddress,
                                 MultipartFile selfie) {
        log.info("KYC submission for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if KYC already exists
        KycVerification kyc = kycRepository.findByUserId(userId)
                .orElse(KycVerification.builder()
                        .user(user)
                        .build());

        // Upload documents (in phase 2 production, upload to cloud storage)
        // String idDocUrl = fileStorageService.uploadFile(idDocument, "kyc/id/" + userId);
        // String addressDocUrl = fileStorageService.uploadFile(proofOfAddress, "kyc/address/" + userId);
        // String selfieUrl = fileStorageService.uploadFile(selfie, "kyc/selfie/" + userId);

        // For demo, we use placeholder URLs
        String idDocUrl = "https://storage.example.com/kyc/id/" + userId;
        String addressDocUrl = "https://storage.example.com/kyc/address/" + userId;
        String selfieUrl = "https://storage.example.com/kyc/selfie/" + userId;

        kyc.setLevel(request.getLevel());
        kyc.setStatus(KycStatus.PENDING);
        kyc.setFullName(request.getFullName());
        kyc.setIdType(request.getIdType());
        kyc.setIdNumber(request.getIdNumber());
        kyc.setDateOfBirth(request.getDateOfBirth());
        kyc.setNationality(request.getNationality());
        kyc.setAddress(request.getAddress());
        kyc.setCity(request.getCity());
        kyc.setState(request.getState());
        kyc.setPostalCode(request.getPostalCode());
        kyc.setCountry(request.getCountry());
        kyc.setIdDocumentUrl(idDocUrl);
        kyc.setProofOfAddressUrl(addressDocUrl);
        kyc.setSelfieUrl(selfieUrl);

        kyc = kycRepository.save(kyc);

        // In phase 3 production: Call external KYC verification service
        // verifyWithExternalService(kyc);

        user.setKycStatus(KycStatus.PENDING);
        userRepository.save(user);

        log.info("KYC submitted successfully for user: {}", userId);
        return mapToResponse(kyc);
    }

    @Override
    @Transactional
    public KycResponse approveKyc(UUID kycId, UUID adminId) {
        log.info("Approving KYC: {}", kycId);

        KycVerification kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC verification not found"));

        kyc.setStatus(KycStatus.VERIFIED);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(LocalDateTime.now());

        kyc = kycRepository.save(kyc);

        // Update user KYC status
        User user = kyc.getUser();
        user.setKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        log.info("KYC approved for user: {}", user.getId());
        return mapToResponse(kyc);
    }

    @Override
    @Transactional
    public KycResponse rejectKyc(UUID kycId, UUID adminId, String reason) {
        log.info("Rejecting KYC: {}", kycId);

        KycVerification kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC verification not found"));

        kyc.setStatus(KycStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(LocalDateTime.now());

        kyc = kycRepository.save(kyc);

        // Update user KYC status
        User user = kyc.getUser();
        user.setKycStatus(KycStatus.REJECTED);
        userRepository.save(user);

        log.info("KYC rejected for user: {}", user.getId());
        return mapToResponse(kyc);
    }

    @Transactional(readOnly = true)
    public KycResponse getUserKyc(UUID userId) {
        KycVerification kyc = kycRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC verification not found"));
        return mapToResponse(kyc);
    }

    @Transactional(readOnly = true)
    public Page<KycResponse> getPendingKyc(Pageable pageable) {
        return kycRepository.findByStatus(KycStatus.PENDING, pageable)
                .map(this::mapToResponse);
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
}

