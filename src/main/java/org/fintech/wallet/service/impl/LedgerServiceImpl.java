package org.fintech.wallet.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.LedgerEntry;
import org.fintech.wallet.domain.enums.EntryType;
import org.fintech.wallet.dto.request.LedgerEntryRequest;
import org.fintech.wallet.repository.LedgerRepository;
import org.fintech.wallet.service.LedgerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;
    @Override
    @Transactional
    public LedgerEntry createEntry(LedgerEntryRequest request) {
        log.info("Creating ledger entry: {} for wallet: {}",
                request.getIdempotencyKey(), request.getWallet().getId());

        // Check idempotency
        if (ledgerRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            log.warn("Duplicate idempotency key detected: {}", request.getIdempotencyKey());
            return ledgerRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow();
        }

        BigDecimal balanceBefore = request.getWallet().getBalance();
        BigDecimal balanceAfter = request.getEntryType() == EntryType.CREDIT
                ? balanceBefore.add(request.getAmount())
                : balanceBefore.subtract(request.getAmount());

        LedgerEntry entry = LedgerEntry.builder()
                .wallet(request.getWallet())
                .entryType(request.getEntryType())
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .transactionReference(request.getTransactionReference())
                .idempotencyKey(request.getIdempotencyKey())
                .description(request.getDescription())
                .externalReference(request.getExternalReference())
                .ipAddress(request.getIpAddress())
                .build();

        entry = ledgerRepository.save(entry);
        log.info("Ledger entry created: {}", entry.getId());

        return entry;
    }
    @Override
    @Transactional(readOnly = true)
    public Page<LedgerEntry> getWalletLedger(UUID walletId, Pageable pageable) {
        return ledgerRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBalance(UUID walletId) {
        BigDecimal credits = ledgerRepository.sumAmountByWalletAndType(walletId, EntryType.CREDIT);
        BigDecimal debits = ledgerRepository.sumAmountByWalletAndType(walletId, EntryType.DEBIT);

        credits = credits != null ? credits : BigDecimal.ZERO;
        debits = debits != null ? debits : BigDecimal.ZERO;

        return credits.subtract(debits);
    }
}
