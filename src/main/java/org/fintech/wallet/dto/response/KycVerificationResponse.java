package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycVerificationResponse {

    private UUID id;

    /* =========================
       USER INFO (Flattened)
       ========================= */
    private UUID userId;
    private String userEmail;

    /* =========================
       KYC STATUS INFO
       ========================= */
    private String level;
    private String status;

    /* =========================
       PERSONAL INFORMATION
       ========================= */
    private String fullName;
    private String idType;
    private String idNumber;
    private LocalDate dateOfBirth;
    private String nationality;

    /* =========================
       ADDRESS INFORMATION
       ========================= */
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    /* =========================
       DOCUMENT URLs
       ========================= */
    private String idDocumentUrl;
    private String proofOfAddressUrl;
    private String selfieUrl;

    /* =========================
       VERIFICATION DETAILS
       ========================= */
    private String verificationProvider;
    private String externalVerificationId;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;

    /* =========================
       AUDIT
       ========================= */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
