package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.event.AuditLogEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogConsumer {

    @KafkaListener(
            topics = "audit-logs",
            groupId = "audit-persistence-group",
            containerFactory = "auditLogEventConcurrentKafkaListenerContainerFactory"
    )
    public void consumeAuditLog(
            AuditLogEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment acknowledgment
    ) {
        if (event == null) {
            log.warn("Audit event is null (partition={}, offset={}, key={})", partition, offset, key);
            acknowledgment.acknowledge();
            return;
        }

        try {
            log.info("Consumed audit log: action={}, entityType={}, entityId={}, userId={}, adminId={}, partition={}, offset={}, key={}",
                    event.getAction(), event.getEntityType(), event.getEntityId(),
                    event.getUserId(), event.getAdminId(), partition, offset, key);

            if (isCriticalAction(event.getAction())) {
                log.warn("CRITICAL AUDIT LOG: action={}, adminId={}, entityId={}, details={}",
                        event.getAction(), event.getAdminId(), event.getEntityId(), event.getDetails());
            }

            // next phase: persist to audit table
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing audit log: action={}, entityId={}", event.getAction(), event.getEntityId(), e);
            // no ack → retry
        }
    }

    private boolean isCriticalAction(String action) {
        if (action == null) return false;
        return action.equals("DELETE_USER") ||
                action.equals("SUSPEND_USER") ||
                action.equals("REVERSE_TRANSACTION") ||
                action.equals("FREEZE_WALLET");
    }
}
