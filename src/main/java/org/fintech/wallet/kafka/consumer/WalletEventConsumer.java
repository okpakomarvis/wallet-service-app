package org.fintech.wallet.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.event.WalletEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventConsumer {

    @KafkaListener(
            topics = "wallet-events",
            groupId = "wallet-processing-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWalletEvent(
            WalletEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing wallet event: Action={}, Currency={}, Wallet={}",
                    event.getAction(), event.getCurrency(), event.getWalletId());

            // Handle different wallet actions
            switch (event.getAction()) {
                case "CREATED":
                    log.info("New wallet created: {} for user: {}",
                            event.getWalletId(), event.getUserId());
                    break;

                case "FROZEN":
                    log.warn("Wallet frozen: {} for user: {}",
                            event.getWalletId(), event.getUserId());
                    break;

                case "BALANCE_UPDATED":
                    log.info("Balance updated: {} -> {} for wallet: {}",
                            event.getOldBalance(), event.getNewBalance(), event.getWalletId());
                    break;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing wallet event", e);
        }
    }
}