package org.fintech.wallet.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycEvent {
    private UUID kycId;
    private UUID userId;
    private String status;
    private String level;
    private String action; // SUBMITTED, APPROVED, REJECTED
    private UUID reviewedBy;
    private LocalDateTime timestamp;
}

