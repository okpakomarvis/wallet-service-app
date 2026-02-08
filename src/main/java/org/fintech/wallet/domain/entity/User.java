package org.fintech.wallet.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.domain.enums.UserRole;
import org.fintech.wallet.domain.enums.UserStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_phone", columnList = "phone_number"),
                @Index(name = "idx_kyc_level", columnList = "kyc_level")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /**
     * Overall KYC process status
     * (PENDING / VERIFIED / REJECTED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus kycStatus;

    /**
     * Highest VERIFIED KYC level for the user
     * Defaults to NONE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", nullable = false, length = 20)
    private KycLevel kycLevel = KycLevel.NONE;
    /**
     * profile image url
     */
    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 255)
    private String profileImagePublicId;

    @Column(length = 50)
    private String profileImageResourceType;

    /* =========================
       SECURITY
       ========================= */

    @Column(nullable = false)
    private Boolean mfaEnabled = false;

    @Column(length = 32)
    private String mfaSecret;

    @Column(length = 100)
    private String transactionPin;

    /* =========================
       AUTHORIZATION
       ========================= */

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<UserRole> roles = new HashSet<>();

    /* =========================
       AUDIT
       ========================= */

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    @Column(length = 45)
    private String lastLoginIp;

    @Version
    private Long version;
}