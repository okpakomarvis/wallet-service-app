package org.fintech.wallet.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.event.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // =========================
    // Topics (single source)
    // =========================
    public static final String TOPIC_TRANSACTION_EVENTS = "transaction-events";
    public static final String TOPIC_NOTIFICATION_EVENTS = "notification-events";
    public static final String TOPIC_KYC_EVENTS = "kyc-events";
    public static final String TOPIC_AUDIT_LOGS = "audit-logs";
    public static final String TOPIC_FRAUD_DETECTION = "fraud-detection";
    public static final String TOPIC_WALLET_EVENTS = "wallet-events";

    // =========================
    // Public API
    // =========================

    public void publishTransactionEvent(TransactionEvent event) {
        require(event, "TransactionEvent");
        String key = firstNonBlank(
                safeUuid(event.getTransactionId()),
                nullSafe(event.getReference())
        );

        sendAsync(TOPIC_TRANSACTION_EVENTS, key, event, "transactionEvent");
    }

    public void publishNotificationEvent(NotificationEvent event) {
        require(event, "NotificationEvent");

        // Key by userId so all notifications for same user partition together (ordering)
        String key = firstNonBlank(
                safeUuid(event.getUserId()),
                nullSafe(event.getTitle())
        );

        sendAsync(TOPIC_NOTIFICATION_EVENTS, key, event, "notificationEvent");
    }

    public void publishKycEvent(KycEvent event) {
        require(event, "KycEvent");

        String key = firstNonBlank(
                safeUuid(event.getKycId()),
                safeUuid(event.getUserId())
        );

        sendAsync(TOPIC_KYC_EVENTS, key, event, "kycEvent");
    }

    public void publishAuditLog(AuditLogEvent event) {
        require(event, "AuditLogEvent");

        String key = firstNonBlank(
                safeUuid(event.getId()),
                nullSafe(event.getEntityType())
        );

        sendAsync(TOPIC_AUDIT_LOGS, key, event, "auditLogEvent");
    }

    public void publishFraudDetectionEvent(FraudDetectionEvent event) {
        require(event, "FraudDetectionEvent");

        String key = firstNonBlank(
                safeUuid(event.getTransactionId()),
                nullSafe(event.getRiskLevel() != null ? event.getRiskLevel() : null)
        );

        sendAsync(TOPIC_FRAUD_DETECTION, key, event, "fraudDetectionEvent");
    }

    public void publishWalletEvent(WalletEvent event) {
        require(event, "WalletEvent");

        String key = firstNonBlank(
                safeUuid(event.getWalletId()),
                safeUuid(event.getUserId()),
                nullSafe(event.getAction())
        );

        sendAsync(TOPIC_WALLET_EVENTS, key, event, "walletEvent");
    }

    // =========================
    // Internal send helper
    // =========================

    private void sendAsync(String topic, String key, Object payload, String label) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Kafka key must not be null or blank for topic: " + topic
            );
        }

        log.info("Kafka publish [{}]: topic={}, key={}", label, topic, key);

        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info(
                                "Kafka publish OK [{}]: topic={}, key={}, partition={}, offset={}",
                                label,
                                topic,
                                key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    } else {
                        log.error(
                                "Kafka publish FAILED [{}]: topic={}, key={}",
                                label,
                                topic,
                                key,
                                ex
                        );
                    }
                });
    }

    // =========================
    // Small helpers
    // =========================

    private static void require(Object o, String name) {
        if (o == null) throw new IllegalArgumentException(name + " must not be null");
    }

    private static String safeUuid(Object id) {
        if (id == null) return null;
        if (id instanceof UUID uuid) return uuid.toString();
        return String.valueOf(id);
    }

    private static String nullSafe(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
