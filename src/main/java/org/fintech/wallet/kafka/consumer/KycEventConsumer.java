package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.dto.event.KycEvent;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KycEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "kyc-events",
            groupId = "kyc-processing-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeKycEvent(
            KycEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing KYC event: Action={}, Status={}, Level={}",
                    event.getAction(), event.getStatus(), event.getLevel());

            // Handle based on action
            switch (event.getAction()) {
                case "SUBMITTED":
                    handleKycSubmitted(event);
                    break;
                case "APPROVED":
                    handleKycApproved(event);
                    break;
                case "REJECTED":
                    handleKycRejected(event);
                    break;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing KYC event", e);
        }
    }

    private void handleKycSubmitted(KycEvent event) {
        SendNotificationRequest notification = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(NotificationType.KYC_SUBMITTED)
                .title("KYC Documents Received")
                .message("We've received your KYC documents. Our team will review them within 24-48 hours.")
                .referenceId(event.getKycId().toString())
                .channel(NotificationChannel.ALL)
                .priority(NotificationPriority.MEDIUM)
                .build();

        notificationService.sendNotification(notification);
    }

    private void handleKycApproved(KycEvent event) {
        String limits = getTransactionLimits(event.getLevel());

        SendNotificationRequest notification = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(NotificationType.KYC_APPROVED)
                .title("KYC Verification Approved! 🎉")
                .message(String.format(
                        "Congratulations! Your KYC verification has been approved. " +
                                "You're now verified for %s with transaction limits: %s",
                        event.getLevel(), limits
                ))
                .referenceId(event.getKycId().toString())
                .channel(NotificationChannel.ALL)
                .priority(NotificationPriority.HIGH)
                .build();

        notificationService.sendNotification(notification);
    }

    private void handleKycRejected(KycEvent event) {
        SendNotificationRequest notification = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(NotificationType.KYC_REJECTED)
                .title("KYC Verification Needs Attention")
                .message("We couldn't verify your KYC documents. Please check your email for details and resubmit.")
                .referenceId(event.getKycId().toString())
                .channel(NotificationChannel.ALL)
                .priority(NotificationPriority.HIGH)
                .build();

        notificationService.sendNotification(notification);
    }

    private String getTransactionLimits(String level) {
        return switch (level) {
            case "TIER_1" -> "up to ₦50,000 per transaction";
            case "TIER_2" -> "up to ₦500,000 per transaction";
            case "TIER_3" -> "unlimited transactions";
            default -> "standard limits";
        };
    }
}

