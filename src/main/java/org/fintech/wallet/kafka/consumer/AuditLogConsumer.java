package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.event.AuditLogEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogConsumer {

    @KafkaListener(
            topics = "audit-logs",
            groupId = "audit-persistence-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuditLog(
            AuditLogEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing audit log: Action={}, Entity={}, User={}, Admin={}",
                    event.getAction(), event.getEntityType(), event.getUserId(), event.getAdminId());

            // next phase In production, persist to dedicated audit log table or external system
            // auditLogRepository.save(event);

            // For critical actions, also log to file for compliance
            if (isCriticalAction(event.getAction())) {
                log.warn("CRITICAL AUDIT LOG: Action={}, Admin={}, Entity={}, Details={}",
                        event.getAction(), event.getAdminId(), event.getEntityId(), event.getDetails());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing audit log", e);
        }
    }

    private boolean isCriticalAction(String action) {
        return action.equals("DELETE_USER") ||
                action.equals("SUSPEND_USER") ||
                action.equals("REVERSE_TRANSACTION") ||
                action.equals("FREEZE_WALLET");
    }
}