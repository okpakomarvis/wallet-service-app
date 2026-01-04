package org.fintech.wallet.service;

import jakarta.transaction.InvalidTransactionException;
import org.fintech.wallet.domain.entity.Transaction;
import org.fintech.wallet.dto.request.TransferRequest;
import org.fintech.wallet.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionService {
    TransactionResponse transfer(TransferRequest request, UUID userId) throws InvalidTransactionException;
    TransactionResponse deposit(UUID walletId, BigDecimal amount, String externalRef, String gateway);
    TransactionResponse withdraw(UUID walletId, BigDecimal amount, String bankAccount);
    Page<TransactionResponse> getUserTransactions(UUID userId, Pageable pageable);
    TransactionResponse getTransactionByReference(String reference);

}
