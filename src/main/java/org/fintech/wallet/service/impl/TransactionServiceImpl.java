package org.fintech.wallet.service.impl;

import jakarta.transaction.InvalidTransactionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.Transaction;
import org.fintech.wallet.domain.entity.Wallet;
import org.fintech.wallet.domain.enums.EntryType;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.domain.enums.TransactionType;
import org.fintech.wallet.dto.event.TransactionEvent;
import org.fintech.wallet.dto.request.LedgerEntryRequest;
import org.fintech.wallet.dto.request.TransferRequest;
import org.fintech.wallet.dto.response.TransactionResponse;
import org.fintech.wallet.exception.InsufficientBalanceException;
import org.fintech.wallet.kafka.KafkaProducerService;
import org.fintech.wallet.repository.TransactionRepository;
import org.fintech.wallet.service.LedgerService;
import org.fintech.wallet.service.TransactionService;
import org.fintech.wallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final LedgerService ledgerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProducerService kafkaProducerService;

    // P2P Transfer with full ACID compliance
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse transfer(TransferRequest request, UUID userId) throws InvalidTransactionException {
        log.info("Processing transfer: {} -> {}, amount: {}",
                request.getSourceWalletId(), request.getDestinationWalletNumber(), request.getAmount());

        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        // Get wallets with pessimistic locking (prevents concurrent modifications)
        Wallet sourceWallet = walletService.getWalletByIdWithLock(request.getSourceWalletId());
        Wallet destinationWallet = walletService.getWalletByIdWithLock(
                walletService.getWalletByNumber(request.getDestinationWalletNumber()).getId()
        );

        // Validate wallet ownership
        if (!sourceWallet.getUser().getId().equals(userId)) {
            throw new InvalidTransactionException("Unauthorized wallet access");
        }

        // Validate currency match
        if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            throw new InvalidTransactionException("Currency mismatch");
        }

        // Validate sufficient balance
        if (sourceWallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        // Create transaction record
        String reference = generateReference("TXN");
        String idempotencyKey = generateIdempotencyKey(reference);

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .sourceWallet(sourceWallet)
                .destinationWallet(destinationWallet)
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .fee(BigDecimal.ZERO)
                .currency(sourceWallet.getCurrency())
                .status(TransactionStatus.PROCESSING)
                .description(request.getDescription())
                .ipAddress(request.getIpAddress())
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Debit source wallet
            LedgerEntryRequest debitEntry = LedgerEntryRequest.builder()
                    .wallet(sourceWallet)
                    .entryType(EntryType.DEBIT)
                    .amount(request.getAmount())
                    .transactionReference(reference)
                    .idempotencyKey(idempotencyKey + "_DEBIT")
                    .description("Transfer to " + destinationWallet.getWalletNumber())
                    .ipAddress(request.getIpAddress())
                    .build();

            ledgerService.createEntry(debitEntry);
            walletService.updateBalance(sourceWallet.getId(), request.getAmount(), false);

            // Credit destination wallet
            LedgerEntryRequest creditEntry = LedgerEntryRequest.builder()
                    .wallet(destinationWallet)
                    .entryType(EntryType.CREDIT)
                    .amount(request.getAmount())
                    .transactionReference(reference)
                    .idempotencyKey(idempotencyKey + "_CREDIT")
                    .description("Transfer from " + sourceWallet.getWalletNumber())
                    .ipAddress(request.getIpAddress())
                    .build();

            ledgerService.createEntry(creditEntry);
            walletService.updateBalance(destinationWallet.getId(), request.getAmount(), true);

            // Update transaction status
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            log.info("Transfer completed successfully: {}", reference);

            // Publish event to Kafka
            //publishTransactionEvent(transaction);
            // After successful transaction, publish event
            TransactionEvent event = TransactionEvent.builder()
                    .transactionId(transaction.getId())
                    .reference(transaction.getReference())
                    .sourceWalletId(sourceWallet.getId())
                    .destinationWalletId(destinationWallet.getId())
                    .userId(userId)
                    .type(transaction.getType())
                    .status(transaction.getStatus())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency().name())
                    .description(transaction.getDescription())
                    .timestamp(LocalDateTime.now())
                    .ipAddress(request.getIpAddress())
                    .eventType("COMPLETED")
                    .build();

            kafkaProducerService.publishTransactionEvent(event);

            return mapToResponse(transaction);

        } catch (Exception e) {
            log.error("Transfer failed: {}", reference, e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
    }

    // Deposit (from external source)
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse deposit(UUID walletId, BigDecimal amount, String externalRef, String gateway) {
        log.info("Processing deposit: wallet={}, amount={}, gateway={}", walletId, amount, gateway);

        Wallet wallet = walletService.getWalletByIdWithLock(walletId);
        String reference = generateReference("DEP");
        String idempotencyKey = generateIdempotencyKey(reference);

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .destinationWallet(wallet)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .currency(wallet.getCurrency())
                .status(TransactionStatus.PROCESSING)
                .externalReference(externalRef)
                .paymentGateway(gateway)
                .description("Deposit via " + gateway)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            LedgerEntryRequest creditEntry = LedgerEntryRequest.builder()
                    .wallet(wallet)
                    .entryType(EntryType.CREDIT)
                    .amount(amount)
                    .transactionReference(reference)
                    .idempotencyKey(idempotencyKey)
                    .externalReference(externalRef)
                    .description("Deposit via " + gateway)
                    .build();

            ledgerService.createEntry(creditEntry);
            walletService.updateBalance(walletId, amount, true);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            log.info("Deposit completed: {}", reference);
            publishTransactionEvent(transaction);

            return mapToResponse(transaction);

        } catch (Exception e) {
            log.error("Deposit failed: {}", reference, e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse withdraw(UUID walletId, BigDecimal amount, String bankAccount) {
        log.info("Processing withdrawal: wallet={}, amount={}", walletId, amount);

        Wallet wallet = walletService.getWalletByIdWithLock(walletId);

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        String reference = generateReference("WTH");
        String idempotencyKey = generateIdempotencyKey(reference);

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .sourceWallet(wallet)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .currency(wallet.getCurrency())
                .status(TransactionStatus.PROCESSING)
                .description("Withdrawal to " + bankAccount)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            LedgerEntryRequest debitEntry = LedgerEntryRequest.builder()
                    .wallet(wallet)
                    .entryType(EntryType.DEBIT)
                    .amount(amount)
                    .transactionReference(reference)
                    .idempotencyKey(idempotencyKey)
                    .description("Withdrawal to " + bankAccount)
                    .build();

            ledgerService.createEntry(debitEntry);
            walletService.updateBalance(walletId, amount, false);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            log.info("Withdrawal completed: {}", reference);
            publishTransactionEvent(transaction);

            return mapToResponse(transaction);

        } catch (Exception e) {
            log.error("Withdrawal failed: {}", reference, e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactions(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String reference) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return mapToResponse(transaction);
    }

    private String generateReference(String prefix) {
        return String.format("%s%d%06d", prefix, System.currentTimeMillis(),
                (int)(Math.random() * 1000000));
    }

    private String generateIdempotencyKey(String reference) {
        return reference + "_" + UUID.randomUUID().toString();
    }

    private void publishTransactionEvent(Transaction transaction) {
        try {
            kafkaTemplate.send("transaction-events", transaction.getReference(),
                    mapToResponse(transaction));
        } catch (Exception e) {
            log.error("Failed to publish transaction event: {}", transaction.getReference(), e);
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .reference(transaction.getReference())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .sourceWalletNumber(transaction.getSourceWallet() != null ?
                        transaction.getSourceWallet().getWalletNumber() : null)
                .destinationWalletNumber(transaction.getDestinationWallet() != null ?
                        transaction.getDestinationWallet().getWalletNumber() : null)
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
