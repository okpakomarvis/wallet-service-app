package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {
    private UserResponse user;
    private List<WalletResponse> wallets;
    private List<TransactionResponse> recentTransactions;
    private Long totalTransactionCount;
    private BigDecimal totalTransactionVolume;
    private Long transactionsLast30Days;
}