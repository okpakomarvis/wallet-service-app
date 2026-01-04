package org.fintech.wallet.dto.response;

import lombok.*;
import org.fintech.wallet.domain.enums.Currency;
import org.fintech.wallet.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // User statistics
    private Long totalUsers;
    private Long activeUsers;
    private Long suspendedUsers;
    private Long lockedUsers;
    private Long verifiedUsers;
    private Long newUsersLast24h;
    private Long newUsersLast7d;
    private Long newUsersLast30d;

    // Wallet statistics
    private Long totalWallets;
    private Long activeWallets;
    private Long frozenWallets;
    private BigDecimal totalBalance;
    private Map<Currency, BigDecimal> balanceByCurrency;

    // Transaction statistics
    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Long pendingTransactions;
    private Long transactionsLast24h;
    private Long transactionsLast7d;
    private Long transactionsLast30d;
    private BigDecimal volumeLast24h;
    private BigDecimal volumeLast7d;
    private BigDecimal volumeLast30d;

    // KYC statistics
    private Long pendingKyc;
    private Long rejectedKyc;

    // Transaction breakdown
    private Map<TransactionType, Long> transactionsByType;

    private LocalDateTime generatedAt;
}


