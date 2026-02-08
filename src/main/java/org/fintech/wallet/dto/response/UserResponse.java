package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.domain.enums.UserRole;
import org.fintech.wallet.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profileImageUrl;
    private UserStatus status;
    private KycStatus kycStatus;
    private Boolean mfaEnabled;
    private Set<UserRole> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
