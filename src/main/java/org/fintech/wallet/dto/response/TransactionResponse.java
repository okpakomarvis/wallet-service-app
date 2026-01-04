package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.Currency;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private String reference;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal fee;
    private Currency currency;
    private TransactionStatus status;
    private String description;
    private String sourceWalletNumber;
    private String destinationWalletNumber;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
