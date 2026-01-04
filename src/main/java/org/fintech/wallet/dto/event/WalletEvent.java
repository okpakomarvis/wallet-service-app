package org.fintech.wallet.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletEvent {
    private UUID walletId;
    private UUID userId;
    private String action; // CREATED, FROZEN, UNFROZEN, BALANCE_UPDATED
    private String currency;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private LocalDateTime timestamp;
}
