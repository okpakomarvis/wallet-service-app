package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.domain.enums.KycStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycResponse {
    private UUID id;
    private UUID userId;
    private KycLevel level;
    private KycStatus status;
    private String fullName;
    private String idType;
    private LocalDate dateOfBirth;
    private String nationality;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
