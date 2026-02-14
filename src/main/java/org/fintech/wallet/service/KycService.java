package org.fintech.wallet.service;

import org.fintech.wallet.dto.request.AdminKycApprovalRequest;
import org.fintech.wallet.dto.request.KycSubmissionRequest;
import org.fintech.wallet.dto.response.KycResponse;
import org.fintech.wallet.dto.response.KycVerificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface KycService {
    KycResponse submitKyc(UUID userId,
                          MultipartFile idDocument,
                          MultipartFile proofOfAddress,
                          MultipartFile selfie);
    KycResponse approveKyc(UUID kycId, UUID adminId,AdminKycApprovalRequest request);
    KycResponse rejectKyc(UUID kycId, UUID adminId, String reason);
    KycResponse getUserKyc(UUID userId);
    Page<KycVerificationResponse> getPendingKyc(Pageable pageable);
}
