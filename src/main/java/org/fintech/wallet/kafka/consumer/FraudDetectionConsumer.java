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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
            containerFactory = "fraudDetectionEventConcurrentKafkaListenerContainerFactory"
    )
    public void consumeFraudEvent(
            FraudDetectionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment
    ) {
        if (event == null) {
            log.warn("Fraud event is null (partition={}, offset={}, key={})", partition, offset, key);
            acknowledgment.acknowledge();
            return;
        }

        try {
            String risk = normalize(event.getRiskLevel());
            log.warn("Consumed fraud event: txId={}, userId={}, riskLevel={}, riskScore={}, partition={}, offset={}, key={}",
                    event.getTransactionId(), event.getUserId(), risk, event.getRiskScore(), partition, offset, key);

            switch (risk) {
                case "CRITICAL" -> handleCriticalRisk(event);
                case "HIGH" -> handleHighRisk(event);
                case "MEDIUM" -> log.info("MEDIUM risk: txId={} (logged for review)", event.getTransactionId());
                case "LOW" -> log.debug("LOW risk: txId={} (analytics only)", event.getTransactionId());
                default -> log.warn("Unknown riskLevel: {}", risk);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing fraud event: txId={}, userId={}", event.getTransactionId(), event.getUserId(), e);
            // no ack → retry
        }
    }

    private void handleCriticalRisk(FraudDetectionEvent event) {
        log.error("CRITICAL RISK DETECTED: Transaction={}", event.getTransactionId());

        // Optional: freeze all user wallets (enable in prod with proper method)
        // walletService.freezeAllUserWallets(event.getUserId());

        send(event, NotificationPriority.URGENT,
                "URGENT: Suspicious Activity Detected",
                String.format("We detected suspicious activity on your account. For your security, we may restrict activity. Transaction: %s %s. Contact support immediately.",
                        event.getAmount(), event.getCurrency()));
    }

    private void handleHighRisk(FraudDetectionEvent event) {
        log.warn("HIGH RISK DETECTED: Transaction={}", event.getTransactionId());

        send(event, NotificationPriority.HIGH,
                "Security Alert: Unusual Activity",
                String.format("We noticed unusual activity on your account. Transaction: %s %s. If this wasn't you, contact support.",
                        event.getAmount(), event.getCurrency()));
    }

    private void send(FraudDetectionEvent event, NotificationPriority priority, String title, String message) {
        if (event.getUserId() == null) throw new IllegalArgumentException("FraudDetectionEvent.userId is required");

        String ref = event.getTransactionId() != null ? event.getTransactionId().toString() : null;

        SendNotificationRequest notification = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(NotificationType.SECURITY_ALERT)
                .title(title)
                .message(message)
                .referenceId(ref)
                .channel(NotificationChannel.ALL)
                .priority(priority)
                .build();

        notificationService.sendNotification(notification);
    }

    private String normalize(Object riskLevel) {
        if (riskLevel == null) return "";
        return String.valueOf(riskLevel).trim().toUpperCase();
    }
}
