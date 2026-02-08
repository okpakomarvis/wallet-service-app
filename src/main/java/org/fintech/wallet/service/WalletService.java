package org.fintech.wallet.service;

import org.fintech.wallet.domain.entity.Wallet;
import org.fintech.wallet.dto.request.CreateWalletRequest;
import org.fintech.wallet.dto.response.WalletResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    WalletResponse createWallet(UUID userId, CreateWalletRequest request);
    void updateBalance(UUID walletId, BigDecimal amount, boolean isCredit);
    Wallet getWalletByIdWithLock(UUID walletId);
    Wallet getWalletById(UUID walletId);
    WalletResponse getWalletByNumberOnly(String walletNumber);
    WalletResponse getWalletByNumber(String walletNumber, UUID userId);
    List<WalletResponse> getUserWallets(UUID userId);
    void freezeWallet(UUID walletId);
    void unfreezeWallet(UUID walletId);
    /**
     * Calculates the total amount the user has transacted today across deposits, withdrawals, and transfers.
     * Only counts SUCCESS transactions.
     */
    BigDecimal getUserDailyTotal(UUID userId);
}
