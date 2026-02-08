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
@Table(
        name = "kyc_verifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_kyc_level",
                        columnNames = {"user_id", "level"}
                )
        },
        indexes = {
                @Index(name = "idx_kyc_user", columnList = "user_id"),
                @Index(name = "idx_kyc_status", columnList = "status"),
                @Index(name = "idx_kyc_level", columnList = "level")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * One user can have MULTIPLE KYC records
     * (one per level: TIER_1, TIER_2, TIER_3)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * KYC tier this verification is for
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycLevel level;

    /**
     * PENDING, VERIFIED, REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus status;

    /* =========================
       PERSONAL INFORMATION
       ========================= */

    @Column(length = 100)
    private String fullName;

    /**
     * PASSPORT, DRIVERS_LICENSE, NATIONAL_ID
     * (Optional — depends on tier policy)
     */
    @Column(length = 50)
    private String idType;

    @Column(length = 50)
    private String idNumber;

    private LocalDate dateOfBirth;

    @Column(length = 100)
    private String nationality;

    /* =========================
       ADDRESS INFORMATION
       ========================= */

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

    /* =========================
       DOCUMENT URLs (Cloudinary)
       ========================= */

    @Column(length = 500)
    private String idDocumentUrl;

    @Column(length = 500)
    private String proofOfAddressUrl;

    @Column(length = 500)
    private String selfieUrl;

    /* =========================
       CLOUDINARY METADATA
       ========================= */

    @Column(length = 300)
    private String idDocumentPublicId;

    @Column(length = 300)
    private String proofOfAddressPublicId;

    @Column(length = 300)
    private String selfiePublicId;

    @Column(length = 30)
    private String idDocumentResourceType;

    @Column(length = 30)
    private String proofOfAddressResourceType;

    @Column(length = 30)
    private String selfieResourceType;

    /* =========================
       VERIFICATION DETAILS
       ========================= */

    /**
     * Manual, Smile Identity, VerifyMe, etc.
     */
    @Column(length = 100)
    private String verificationProvider;

    @Column(length = 100)
    private String externalVerificationId;

    private LocalDateTime verifiedAt;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy; // Admin user ID

    private LocalDateTime reviewedAt;

    /* =========================
       AUDIT
       ========================= */

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}