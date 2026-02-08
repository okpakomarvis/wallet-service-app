package org.fintech.wallet.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectionEvent {
    private UUID transactionId;
    private UUID userId;
    private String eventType;
    private BigDecimal amount;
    private String currency;
    private String ipAddress;
    private String deviceFingerprint;
    private Integer riskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private LocalDateTime timestamp;
}