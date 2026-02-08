package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.enums.*;
import org.fintech.wallet.dto.event.TransactionEvent;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "transaction-notification-group",
            containerFactory = "transactionEventKafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment
    ) {

        if (event == null) {
            log.warn("Transaction event is null (partition={}, offset={}, key={})",
                    partition, offset, key);
            acknowledgment.acknowledge();
            return;
        }

        try {
            log.info(
                    "Consumed transaction event: ref={}, type={}, status={}, userId={}, partition={}, offset={}, key={}",
                    event.getReference(),
                    event.getType(),
                    event.getStatus(),
                    event.getUserId(),
                    partition,
                    offset,
                    key
            );

            if (event.getUserId() == null) {
                throw new IllegalArgumentException("TransactionEvent.userId is required");
            }

            NotificationType notificationType = mapToNotificationType(event);

            SendNotificationRequest notificationRequest =
                    SendNotificationRequest.builder()
                            .userId(event.getUserId())
                            .type(notificationType)
                            .title(getNotificationTitle(event))
                            .message(getNotificationMessage(event))
                            .referenceId(
                                    event.getTransactionId() != null
                                            ? event.getTransactionId().toString()
                                            : event.getReference()
                            )
                            .channel(NotificationChannel.ALL)
                            .priority(getNotificationPriority(event))
                            .build();

            notificationService.sendNotification(notificationRequest);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error(
                    "Error processing transaction event: ref={}, key={}, partition={}, offset={}",
                    event.getReference(),
                    key,
                    partition,
                    offset,
                    e
            );
            // no ack → retry (DLT recommended at container level)
        }
    }

    /* --------------------------------------------------------------------- */
    /* Mapping logic                                                          */
    /* --------------------------------------------------------------------- */

    private NotificationType mapToNotificationType(TransactionEvent event) {

        if (event.getType() == null || event.getStatus() == null) {
            return NotificationType.TRANSACTION_PENDING;
        }

        return switch (event.getType()) {

            case DEPOSIT -> switch (event.getStatus()) {
                case SUCCESS -> NotificationType.DEPOSIT_SUCCESS;
                case FAILED -> NotificationType.DEPOSIT_FAILED;
                case PENDING -> NotificationType.DEPOSIT_PENDING;
                default -> NotificationType.TRANSACTION_PENDING;
            };

            case WITHDRAWAL -> switch (event.getStatus()) {
                case SUCCESS -> NotificationType.WITHDRAWAL_SUCCESS;
                case FAILED -> NotificationType.WITHDRAWAL_FAILED;
                case PENDING -> NotificationType.WITHDRAWAL_PENDING;
                default -> NotificationType.TRANSACTION_PENDING;
            };

            case TRANSFER -> {
                if ("RECEIVED".equalsIgnoreCase(event.getEventType())) {
                    yield NotificationType.TRANSFER_RECEIVED;
                }

                yield switch (event.getStatus()) {
                    case SUCCESS -> NotificationType.TRANSFER_SENT;
                    case FAILED -> NotificationType.TRANSFER_FAILED;
                    case PENDING -> NotificationType.TRANSFER_PENDING;
                    default -> NotificationType.TRANSACTION_PENDING;
                };
            }

            case REVERSAL -> NotificationType.TRANSACTION_REVERSED;

            default -> switch (event.getStatus()) {
                case SUCCESS -> NotificationType.TRANSACTION_SUCCESS;
                case FAILED -> NotificationType.TRANSACTION_FAILED;
                case PENDING -> NotificationType.TRANSACTION_PENDING;
                default -> NotificationType.TRANSACTION_PENDING;
            };
        };
    }

    /* --------------------------------------------------------------------- */
    /* Notification presentation                                              */
    /* --------------------------------------------------------------------- */

    private String getNotificationTitle(TransactionEvent event) {
        if (event.getStatus() == null) return "Transaction Update";

        return switch (event.getStatus()) {
            case SUCCESS -> "Transaction Successful ✓";
            case FAILED -> "Transaction Failed ✗";
            case PENDING -> "Transaction Pending";
            case REVERSED -> "Transaction Reversed";
            default -> "Transaction Update";
        };
    }

    private String getNotificationMessage(TransactionEvent event) {

        String transactionType =
                event.getType() != null
                        ? event.getType().name().toLowerCase().replace("_", " ")
                        : "transaction";

        String status =
                event.getStatus() != null
                        ? event.getStatus().name().toLowerCase()
                        : "updated";

        BigDecimal amount =
                event.getAmount() != null ? event.getAmount() : BigDecimal.ZERO;

        String currency =
                event.getCurrency() != null ? event.getCurrency() : "";
        String transactionStatus = event.getEventType() != null ? event.getEventType() : "";
        String description =  event.getDescription() != null ? event.getDescription() : "";
        if(transactionStatus.equals(TransactionStatus.RECEIVED.name())){
            return String.format(
                "%s. Reference: %s",
                description,
                event.getReference()
            );
        }

        return String.format(
                "Your %s of %s %s has been %s. Reference: %s",
                transactionType,
                formatAmount(amount),
                currency,
                status,
                event.getReference()
        );
    }

    /* --------------------------------------------------------------------- */
    /* Priority logic                                                         */
    /* --------------------------------------------------------------------- */

    private NotificationPriority getNotificationPriority(TransactionEvent event) {

        if (event.getStatus() == TransactionStatus.FAILED) {
            return NotificationPriority.HIGH;
        }

        if (event.getStatus() == TransactionStatus.REVERSED) {
            return NotificationPriority.HIGH;
        }

        BigDecimal amount =
                event.getAmount() != null ? event.getAmount() : BigDecimal.ZERO;

        if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            return NotificationPriority.HIGH;
        }

        return NotificationPriority.MEDIUM;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}
