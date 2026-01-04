package org.fintech.wallet.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.event.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Transaction Events
    public void publishTransactionEvent(TransactionEvent event) {
        String topic = "transaction-events";
        String key = event.getTransactionId().toString();

        log.info("Publishing transaction event: {} to topic: {}", key, topic);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transaction event published successfully: {} to partition: {}",
                        key, result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish transaction event: {}", key, ex);
            }
        });
    }

    // Notification Events
    public void publishNotificationEvent(NotificationEvent event) {
        String topic = "notification-events";
        String key = event.getUserId().toString();

        log.info("Publishing notification event for user: {}", key);

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Notification event published: {}", key);
                    } else {
                        log.error("Failed to publish notification event: {}", key, ex);
                    }
                });
    }

    // KYC Events
    public void publishKycEvent(KycEvent event) {
        String topic = "kyc-events";
        String key = event.getKycId().toString();

        log.info("Publishing KYC event: {} - {}", key, event.getAction());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("KYC event published: {}", key);
                    } else {
                        log.error("Failed to publish KYC event: {}", key, ex);
                    }
                });
    }

    // Audit Logs
    public void publishAuditLog(AuditLogEvent event) {
        String topic = "audit-logs";
        String key = event.getId().toString();

        log.info("Publishing audit log: {} - {}", event.getAction(), event.getEntityType());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish audit log", ex);
                    }
                });
    }

    // Fraud Detection
    public void publishFraudDetectionEvent(FraudDetectionEvent event) {
        String topic = "fraud-detection";
        String key = event.getTransactionId().toString();

        log.warn("Publishing fraud detection event: {} - Risk: {}",
                key, event.getRiskLevel());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Fraud detection event published: {}", key);
                    } else {
                        log.error("Failed to publish fraud detection event: {}", key, ex);
                    }
                });
    }

    // Wallet Events
    public void publishWalletEvent(WalletEvent event) {
        String topic = "wallet-events";
        String key = event.getWalletId().toString();

        log.info("Publishing wallet event: {} - {}", key, event.getAction());

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish wallet event: {}", key, ex);
                    }
                });
    }
}
