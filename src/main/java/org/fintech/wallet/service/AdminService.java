package org.fintech.wallet.service;

import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.domain.enums.TransactionType;
import org.fintech.wallet.domain.enums.UserStatus;
import org.fintech.wallet.dto.request.AdminActionRequest;
import org.fintech.wallet.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AdminService {
    DashboardStatsResponse getDashboardStats();
    Page<UserResponse> getAllUsers(Pageable pageable);
    AdminUserDetailResponse getUserDetails(UUID userId);
    UserResponse suspendUser(UUID userId, UUID adminId, AdminActionRequest request);
    UserResponse unsuspendUser(UUID userId, UUID adminId, String note);
    UserResponse lockUser(UUID userId, UUID adminId, AdminActionRequest request);
    UserResponse unlockUser(UUID userId, UUID adminId, String note);
    void deleteUser(UUID userId, UUID adminId, AdminActionRequest request);
    Page<TransactionResponse> searchTransactions(
            String reference,
            UUID userId,
            TransactionStatus status,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
    TransactionDetailResponse getTransactionDetails(String reference);
    TransactionResponse reverseTransaction(String reference, UUID adminId,
                                           AdminActionRequest request);
    WalletResponse freezeWallet(UUID walletId, UUID adminId, AdminActionRequest request);
    WalletResponse unfreezeWallet(UUID walletId, UUID adminId, String note);
    TransactionReportResponse generateTransactionReport(
            LocalDateTime startDate,
            LocalDateTime endDate);

    Page<UserResponse> searchUsers(String searchTerm, UserStatus status,
                                   KycStatus kycStatus, Pageable pageable);
}
