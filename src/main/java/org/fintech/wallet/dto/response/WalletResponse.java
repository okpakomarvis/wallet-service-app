package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.Currency;
import org.fintech.wallet.domain.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID id;
    private String walletNumber;
    private Currency currency;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private WalletStatus status;
    private LocalDateTime createdAt;
}
