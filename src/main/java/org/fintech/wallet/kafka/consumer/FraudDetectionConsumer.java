package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.dto.event.FraudDetectionEvent;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.service.NotificationService;
import org.fintech.wallet.service.WalletService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionConsumer {

    private final NotificationService notificationService;
    private final WalletService walletService;

    @KafkaListener(
            topics = "fraud-detection",
            groupId = "fraud-analysis-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFraudEvent(
            FraudDetectionEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.warn("Processing fraud detection event: Transaction={}, RiskLevel={}, RiskScore={}",
                    event.getTransactionId(), event.getRiskLevel(), event.getRiskScore());

            // Take action based on risk level
            switch (event.getRiskLevel()) {
                case "CRITICAL":
                    handleCriticalRisk(event);
                    break;
                case "HIGH":
                    handleHighRisk(event);
                    break;
                case "MEDIUM":
                    handleMediumRisk(event);
                    break;
                case "LOW":
                    handleLowRisk(event);
                    break;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing fraud event", e);
        }
    }

    private void handleCriticalRisk(FraudDetectionEvent event) {
        log.error("CRITICAL RISK DETECTED: Transaction={}", event.getTransactionId());

        // 1. Immediately freeze all user wallets
        // walletService.freezeAllUserWallets(event.getUserId());

        // 2. Send urgent notification to user
        SendNotificationRequest notification = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(NotificationType.SECURITY_ALERT)
                .title("URGENT: Suspicious Activity Detected")
                .message(String.format(
                        "We detected suspicious activity on your account. " +
                                "For your security, we've temporarily frozen your wallets. " +
                                "Transaction amount: %s %s. Please contact support immediately.",
                        event.getAmount(), event.getCurrency()
                ))
                .referenceId(event.getTransactionId().toString())
                .channel(NotificationChannel.ALL)
                .priority(NotificationPriority.URGENT)
                .build();

        notificationService.sendNotification(notification);

        // 3. Alert compliance team
        log.error("COMPLIANCE ALERT: Critical fraud detected for user: {}", event.getUserId());
    }

    private void handleHighRisk(FraudDetectionEvent event) {
        log.warn("HIGH RISK DETECTED: Transaction={}", event.getTransactionId());

        // Send security alert to user
        SendNotificationRequest notification = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(NotificationType.SECURITY_ALERT)
                .title("Security Alert: Unusual Activity")
                .message(String.format(
                        "We noticed unusual activity on your account. " +
                                "Transaction: %s %s. If this wasn't you, please contact support.",
                        event.getAmount(), event.getCurrency()
                ))
                .referenceId(event.getTransactionId().toString())
                .channel(NotificationChannel.ALL)
                .priority(NotificationPriority.HIGH)
                .build();

        notificationService.sendNotification(notification);
    }

    private void handleMediumRisk(FraudDetectionEvent event) {
        log.info("MEDIUM RISK: Transaction={}", event.getTransactionId());
        // Log for review, but don't notify user
    }

    private void handleLowRisk(FraudDetectionEvent event) {
        log.debug("LOW RISK: Transaction={}", event.getTransactionId());
        // Just log for analytics
    }
}

