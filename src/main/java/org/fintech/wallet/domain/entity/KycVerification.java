package org.fintech.wallet.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.domain.enums.KycStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_verifications", indexes = {
        @Index(name = "idx_kyc_user", columnList = "user_id"),
        @Index(name = "idx_kyc_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycLevel level; // TIER_1, TIER_2, TIER_3

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status;

    // Personal Information
    @Column(length = 100)
    private String fullName;

    @Column(length = 50)
    private String idType; // PASSPORT, DRIVERS_LICENSE, NATIONAL_ID

    @Column(length = 50)
    private String idNumber;

    private LocalDate dateOfBirth;

    @Column(length = 100)
    private String nationality;

    // Address Information
    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 50)
    private String country;

    // Document URLs (stored Cloudinary URLs)
    @Column(length = 500)
    private String idDocumentUrl;

    @Column(length = 500)
    private String proofOfAddressUrl;

    @Column(length = 500)
    private String selfieUrl;

    // Verification Details
    @Column(length = 100)
    private String verificationProvider; // ( e.g Smile Identity, Verify.me)

    @Column(length = 100)
    private String externalVerificationId;

    private LocalDateTime verifiedAt;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy; // Admin user ID

    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

