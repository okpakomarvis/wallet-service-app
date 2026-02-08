package org.fintech.wallet.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.entity.Wallet;
import org.fintech.wallet.domain.enums.EntryType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryRequest {
    private Wallet wallet;
    private EntryType entryType;
    private BigDecimal amount;
    private String transactionReference;
    private String idempotencyKey;
    private String description;
    private String externalReference;
    private String ipAddress;
}
