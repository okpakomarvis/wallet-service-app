package org.fintech.wallet.service;

import org.fintech.wallet.domain.entity.LedgerEntry;
import org.fintech.wallet.dto.request.LedgerEntryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerService {
    LedgerEntry createEntry(LedgerEntryRequest request);
    Page<LedgerEntry> getWalletLedger(UUID walletId, Pageable pageable);
    BigDecimal calculateBalance(UUID walletId);
}
