package org.fintech.wallet.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesRequest {

    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;

    private Boolean transactionAlerts;
    private Boolean securityAlerts;
    private Boolean marketingAlerts;
    private Boolean systemAlerts;
}
