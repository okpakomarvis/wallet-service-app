package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.dto.event.TransactionEvent;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "transaction-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Consumed transaction event: {} from partition: {}, offset: {}",
                    event.getReference(), partition, offset);

            // Get notification details
            String title = getNotificationTitle(event);
            String message = getNotificationMessage(event);
            NotificationType notificationType = mapToNotificationType(event);

            // Send notification to user
            SendNotificationRequest notificationRequest = SendNotificationRequest.builder()
                    .userId(event.getUserId())
                    .type(notificationType)
                    .title(title)
                    .message(message)
                    .referenceId(event.getTransactionId().toString())
                    .channel(NotificationChannel.ALL) // Send via all channels
                    .priority(getNotificationPriority(event))
                    .build();

            notificationService.sendNotification(notificationRequest);

            log.info("Notification sent for transaction: {}", event.getReference());

            // Manually commit offset after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing transaction event: {}", event.getReference(), e);
            // Don't acknowledge - message will be retried
        }
    }

    /**
     * Maps transaction event to appropriate notification type
     */
    private NotificationType mapToNotificationType(TransactionEvent event) {
        // Map based on transaction type and status
        return switch (event.getType()) {
            case DEPOSIT -> switch (event.getStatus()) {
                case SUCCESS -> NotificationType.DEPOSIT_SUCCESS;
                case FAILED -> NotificationType.DEPOSIT_FAILED;
                case PENDING -> NotificationType.DEPOSIT_PENDING;
                default -> NotificationType.TRANSACTION_SUCCESS;
            };

            case WITHDRAWAL -> switch (event.getStatus()) {
                case SUCCESS -> NotificationType.WITHDRAWAL_SUCCESS;
                case FAILED -> NotificationType.WITHDRAWAL_FAILED;
                case PENDING -> NotificationType.WITHDRAWAL_PENDING;
                default -> NotificationType.TRANSACTION_SUCCESS;
            };

            case TRANSFER -> {
                // Check if this is sender or receiver
                if (event.getEventType() != null && event.getEventType().equals("RECEIVED")) {
                    yield NotificationType.TRANSFER_RECEIVED;
                } else if (event.getStatus() == TransactionStatus.SUCCESS) {
                    yield NotificationType.TRANSFER_SENT;
                } else {
                    yield NotificationType.TRANSFER_FAILED;
                }
            }

            case REVERSAL -> NotificationType.TRANSACTION_REVERSED;

            default -> switch (event.getStatus()) {
                case SUCCESS -> NotificationType.TRANSACTION_SUCCESS;
                case FAILED -> NotificationType.TRANSACTION_FAILED;
                case PENDING -> NotificationType.TRANSACTION_PENDING;
                default -> NotificationType.TRANSACTION_SUCCESS;
            };
        };
    }

    /**
     * Generates user-friendly notification title
     */
    private String getNotificationTitle(TransactionEvent event) {
        return switch (event.getStatus()) {
            case SUCCESS -> "Transaction Successful ✓";
            case FAILED -> "Transaction Failed ✗";
            case PENDING -> "Transaction Pending";
            case REVERSED -> "Transaction Reversed";
            default -> "Transaction Update";
        };
    }

    /**
     * Generates detailed notification message
     */
    private String getNotificationMessage(TransactionEvent event) {
        String transactionType = event.getType().name().toLowerCase().replace("_", " ");
        String status = event.getStatus().name().toLowerCase();

        return String.format(
                "Your %s of %s %s has been %s. Reference: %s",
                transactionType,
                formatAmount(event.getAmount()),
                event.getCurrency(),
                status,
                event.getReference()
        );
    }

    /**
     * Determines notification priority based on transaction
     */
    private NotificationPriority getNotificationPriority(TransactionEvent event) {
        // High priority for failures and high-value transactions
        if (event.getStatus() == TransactionStatus.FAILED) {
            return NotificationPriority.HIGH;
        }

        // High priority for large transactions (over 100,000)
        if (event.getAmount().compareTo(new java.math.BigDecimal("100000")) > 0) {
            return NotificationPriority.HIGH;
        }

        // Medium priority for reversals
        if (event.getStatus() == TransactionStatus.REVERSED) {
            return NotificationPriority.MEDIUM;
        }

        // Default to medium priority
        return NotificationPriority.MEDIUM;
    }

    /**
     * Formats amount with proper currency symbol
     */
    private String formatAmount(java.math.BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}
