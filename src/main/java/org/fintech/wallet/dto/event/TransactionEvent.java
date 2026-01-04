package org.fintech.wallet.dto.event;

import lombok.*;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private UUID transactionId;
    private String reference;
    private UUID sourceWalletId;
    private UUID destinationWalletId;
    private UUID userId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String eventType; // CREATED, COMPLETED, FAILED, REVERSED
}
