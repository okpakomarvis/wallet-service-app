package org.fintech.wallet.dto.response;

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
public class NotificationPreferencesResponse {
    private UUID userId;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean transactionAlerts;
    private Boolean securityAlerts;
    private Boolean marketingAlerts;
    private Boolean systemAlerts;
    private LocalDateTime updatedAt;
}
