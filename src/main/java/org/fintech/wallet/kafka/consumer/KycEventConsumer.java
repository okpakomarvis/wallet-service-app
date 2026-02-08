package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.enums.KycLevel;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.dto.event.KycEvent;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KycEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "kyc-events",
            groupId = "kyc-processing-group",
            containerFactory = "kycEventKafkaListenerContainerFactory"
    )
    public void consumeKycEvent(
            KycEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment
    ) {
        if (event == null) {
            log.warn("KYC event is null (partition={}, offset={}, key={})", partition, offset, key);
            acknowledgment.acknowledge();
            return;
        }

        try {
            String action = safe(event.getAction());
            log.info("Consumed KYC event: action={}, status={}, level={}, userId={}, kycId={}, partition={}, offset={}, key={}",
                    action, event.getStatus(), event.getLevel(), event.getUserId(), event.getKycId(), partition, offset, key);

            switch (action) {
                case "SUBMITTED" -> handleKycSubmitted(event);
                case "APPROVED" -> handleKycApproved(event);
                case "REJECTED" -> handleKycRejected(event);
                default -> log.warn("Unknown KYC action: {}", action);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing KYC event: userId={}, kycId={}", event.getUserId(), event.getKycId(), e);
            // no ack → retry
        }
    }

    private void handleKycSubmitted(KycEvent event) {
        send(event, NotificationType.KYC_SUBMITTED,
                "KYC Documents Received",
                "We've received your KYC documents. Our team will review them within 24-48 hours.",
                NotificationPriority.MEDIUM);
    }

    private void handleKycApproved(KycEvent event) {
        String limits = getTransactionLimits(event.getLevel());
        send(event, NotificationType.KYC_APPROVED,
                "KYC Verification Approved! 🎉",
                String.format("Congratulations! Your KYC verification has been approved. You're now verified for %s with transaction limits: %s",
                        event.getLevel(), limits),
                NotificationPriority.HIGH);
    }

    private void handleKycRejected(KycEvent event) {
        send(event, NotificationType.KYC_REJECTED,
                "KYC Verification Needs Attention",
                "We couldn't verify your KYC documents. Please check your email for details and resubmit.",
                NotificationPriority.HIGH);
    }

    private void send(KycEvent event, NotificationType type, String title, String message, NotificationPriority priority) {
        if (event.getUserId() == null) throw new IllegalArgumentException("KycEvent.userId is required");

        String ref = event.getKycId() != null ? event.getKycId().toString() : null;

        SendNotificationRequest notification = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(type)
                .title(title)
                .message(message)
                .referenceId(ref)
                .channel(NotificationChannel.ALL)
                .priority(priority)
                .build();

        notificationService.sendNotification(notification);
    }

    private String getTransactionLimits(String level) {
        if (level == null) return "standard limits";
        return switch (level) {
            case "TIER_1" -> "up to ₦"+KycLevel.valueOf("TIER_1").getPerTransactionLimit()+" per transaction";
            case "TIER_2" -> "up to ₦"+KycLevel.valueOf("TIER_2").getPerTransactionLimit()+" per transaction";
            case "TIER_3" -> "unlimited transactions";
            default -> "standard limits";
        };
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
