package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.dto.event.WalletEvent;
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
public class WalletEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "wallet-events",
            groupId = "wallet-processing-group",
            containerFactory = "walletEventKafkaListenerContainerFactory"
    )
    public void consumeWalletEvent(
            WalletEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment
    ) {
        if (event == null) {
            log.warn("Wallet event is null (partition={}, offset={}, key={})", partition, offset, key);
            acknowledgment.acknowledge();
            return;
        }

        try {
            String action = safe(event.getAction());
            log.info("Consumed wallet event: action={}, walletId={}, userId={}, currency={}, partition={}, offset={}, key={}",
                    action, event.getWalletId(), event.getUserId(), event.getCurrency(), partition, offset, key);

            switch (action) {
                case "CREATED" -> notifyUser(event, NotificationType.WALLET_CREATED,
                        "Wallet Created",
                        "Your " + safe(event.getCurrency()) + " wallet has been created successfully.");

                case "FROZEN" -> notifyUser(event, NotificationType.WALLET_FROZEN,
                        "Wallet Frozen",
                        "Your wallet has been frozen for security reasons. Contact support if needed.");

                case "UNFROZEN" -> notifyUser(event, NotificationType.WALLET_UNFROZEN,
                        "Wallet Unfrozen",
                        "Your wallet has been unfrozen and is now active.");

                case "LOW_BALANCE" -> notifyUser(event, NotificationType.WALLET_LOW_BALANCE,
                        "Low Balance Alert",
                        "Your wallet balance is low. Consider funding your wallet.");

                case "BALANCE_UPDATED" -> {
                    // Usually too noisy; keep as log
                    log.info("Wallet balance updated: walletId={}, old={}, new={}",
                            event.getWalletId(), event.getOldBalance(), event.getNewBalance());
                }

                default -> log.warn("Unknown wallet action: action={}, walletId={}, userId={}",
                        action, event.getWalletId(), event.getUserId());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing wallet event (walletId={}, userId={})",
                    event.getWalletId(), event.getUserId(), e);
            // do NOT ack → Kafka retries
        }
    }

    private void notifyUser(WalletEvent event, NotificationType type, String title, String message) {
        if (event.getUserId() == null) {
            log.warn("Cannot notify user: userId is null for wallet event walletId={}", event.getWalletId());
            return;
        }
        SendNotificationRequest req = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(type)
                .title(title)
                .message(message)
                .referenceId(event.getWalletId() != null ? event.getWalletId().toString() : null)
                .channel(NotificationChannel.IN_APP) // wallet updates best as in-app
                .priority(NotificationPriority.MEDIUM)
                .build();

        notificationService.sendNotification(req);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
