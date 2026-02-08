package org.fintech.wallet.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.EntryType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_wallet_created", columnList = "wallet_id, created_at"),
        @Index(name = "idx_transaction_ref", columnList = "transaction_reference"),
        @Index(name = "idx_idempotency", columnList = "idempotency_key", unique = true)
})
@Immutable // Cannot be updated after creation
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType; // DEBIT or CREDIT

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 100)
    private String transactionReference;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey; // Prevent duplicate transactions

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String externalReference; // Reference from payment gateway

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 45)
    private String ipAddress;
}