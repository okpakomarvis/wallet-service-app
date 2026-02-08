package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.LedgerEntry;
import org.fintech.wallet.domain.entity.Transaction;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.entity.Wallet;
import org.fintech.wallet.domain.enums.*;
import org.fintech.wallet.domain.enums.Currency;
import org.fintech.wallet.dto.request.AdminActionRequest;
import org.fintech.wallet.dto.response.*;
import org.fintech.wallet.exception.UserNotFoundException;
import org.fintech.wallet.repository.LedgerRepository;
import org.fintech.wallet.repository.TransactionRepository;
import org.fintech.wallet.repository.UserRepository;
import org.fintech.wallet.repository.WalletRepository;
import org.fintech.wallet.service.AdminService;
import org.fintech.wallet.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final NotificationService notificationService;

    /**
     * DASHBOARD & STATISTICS
     * @return
     */

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        log.info("Fetching comprehensive dashboard statistics");

        // User Statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long lockedUsers = userRepository.countByStatus(UserStatus.LOCKED);
        long verifiedUsers = userRepository.countByKycStatus(KycStatus.VERIFIED);

        // Wallet Statistics
        long totalWallets = walletRepository.count();
        long activeWallets = walletRepository.countByStatus(WalletStatus.ACTIVE);
        long frozenWallets = walletRepository.countByStatus(WalletStatus.FROZEN);

        // Calculate total balance across all wallets
        List<Wallet> allWallets = walletRepository.findAll();
        BigDecimal totalBalance = allWallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Balance by currency
        Map<org.fintech.wallet.domain.enums.Currency, BigDecimal> balanceByCurrency = allWallets.stream()
                .collect(Collectors.groupingBy(
                        Wallet::getCurrency,
                        Collectors.mapping(
                                Wallet::getBalance,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // Transaction Statistics
        long totalTransactions = transactionRepository.count();
        long successfulTransactions = transactionRepository.countByStatus(TransactionStatus.SUCCESS);
        long failedTransactions = transactionRepository.countByStatus(TransactionStatus.FAILED);
        long pendingTransactions = transactionRepository.countByStatus(TransactionStatus.PENDING);

        // Time-based statistics
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24Hours = now.minus(24, ChronoUnit.HOURS);
        LocalDateTime last7Days = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime last30Days = now.minus(30, ChronoUnit.DAYS);

        long transactionsLast24h = transactionRepository.countTransactionsSince(last24Hours);
        long transactionsLast7d = transactionRepository.countTransactionsSince(last7Days);
        long transactionsLast30d = transactionRepository.countTransactionsSince(last30Days);

        // Transaction volume (amount)
        BigDecimal volumeLast24h = transactionRepository.sumTransactionAmountSince(last24Hours);
        BigDecimal volumeLast7d = transactionRepository.sumTransactionAmountSince(last7Days);
        BigDecimal volumeLast30d = transactionRepository.sumTransactionAmountSince(last30Days);

        // New users statistics
        long newUsersLast24h = userRepository.countCreatedSince(last24Hours);
        long newUsersLast7d = userRepository.countCreatedSince(last7Days);
        long newUsersLast30d = userRepository.countCreatedSince(last30Days);

        // KYC Statistics
        long pendingKyc = userRepository.countByKycStatus(KycStatus.PENDING);
        long rejectedKyc = userRepository.countByKycStatus(KycStatus.REJECTED);

        // Transaction type breakdown
        Map<TransactionType, Long> transactionsByType = Arrays.stream(TransactionType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> transactionRepository.countByType(type)
                ));

        return DashboardStatsResponse.builder()
                // User stats
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .lockedUsers(lockedUsers)
                .verifiedUsers(verifiedUsers)
                .newUsersLast24h(newUsersLast24h)
                .newUsersLast7d(newUsersLast7d)
                .newUsersLast30d(newUsersLast30d)

                // Wallet stats
                .totalWallets(totalWallets)
                .activeWallets(activeWallets)
                .frozenWallets(frozenWallets)
                .totalBalance(totalBalance)
                .balanceByCurrency(balanceByCurrency)

                // Transaction stats
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .pendingTransactions(pendingTransactions)
                .transactionsLast24h(transactionsLast24h)
                .transactionsLast7d(transactionsLast7d)
                .transactionsLast30d(transactionsLast30d)
                .volumeLast24h(volumeLast24h != null ? volumeLast24h : BigDecimal.ZERO)
                .volumeLast7d(volumeLast7d != null ? volumeLast7d : BigDecimal.ZERO)
                .volumeLast30d(volumeLast30d != null ? volumeLast30d : BigDecimal.ZERO)

                // KYC stats
                .pendingKyc(pendingKyc)
                .rejectedKyc(rejectedKyc)

                // Transaction breakdown
                .transactionsByType(transactionsByType)

                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * USER MANAGEMENT
     * @param pageable
     * @return
     */

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching all users - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String searchTerm, UserStatus status,
                                          KycStatus kycStatus, Pageable pageable) {
        log.info("Searching users: term={}, status={}, kycStatus={}", searchTerm, status, kycStatus);

        if (searchTerm != null && !searchTerm.isEmpty()) {
            return userRepository.searchByEmailOrName(searchTerm, pageable)
                    .map(this::mapToUserResponse);
        }

        if (status != null && kycStatus != null) {
            return userRepository.findByStatusAndKycStatus(status, kycStatus, pageable)
                    .map(this::mapToUserResponse);
        }

        if (status != null) {
            return userRepository.findByStatus(status, pageable)
                    .map(this::mapToUserResponse);
        }

        if (kycStatus != null) {
            return userRepository.findByKycStatus(kycStatus, pageable)
                    .map(this::mapToUserResponse);
        }

        return getAllUsers(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetails(UUID userId) {
        log.info("Fetching detailed user information: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Get user wallets
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        List<WalletResponse> walletResponses = wallets.stream()
                .map(this::mapToWalletResponse)
                .collect(Collectors.toList());

        // Get recent transactions
        List<Transaction> recentTransactions = transactionRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId);
        List<TransactionResponse> transactionResponses = recentTransactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        // Calculate user statistics
        long totalTransactionCount = transactionRepository.countByUserId(userId);
        BigDecimal totalTransactionVolume = transactionRepository.sumTransactionVolumeByUserId(userId);

        LocalDateTime last30Days = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        long transactionsLast30d = transactionRepository.countByUserIdSince(userId, last30Days);

        return AdminUserDetailResponse.builder()
                .user(mapToUserResponse(user))
                .wallets(walletResponses)
                .recentTransactions(transactionResponses)
                .totalTransactionCount(totalTransactionCount)
                .totalTransactionVolume(totalTransactionVolume != null ? totalTransactionVolume : BigDecimal.ZERO)
                .transactionsLast30Days(transactionsLast30d)
                .build();
    }

    @Override
    @Transactional
    public UserResponse suspendUser(UUID userId, UUID adminId, AdminActionRequest request) {
        log.info("Suspending user: {} by admin: {}, reason: {}", userId, adminId, request.getReason());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("User is already suspended");
        }

        // Update user status
        user.setStatus(UserStatus.SUSPENDED);
        user = userRepository.save(user);

        // Freeze all user wallets
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        for (Wallet wallet : wallets) {
            wallet.setStatus(WalletStatus.FROZEN);
            walletRepository.save(wallet);
        }

        // Send notification
        notificationService.sendNotification(
                userId,
                NotificationType.ACCOUNT_SUSPENDED,
                "Account Suspended",
                "Your account has been suspended. Reason: " + request.getReason() +
                        ". Please contact support for assistance.",
                null
        );

        // Log admin action
        logAdminAction(adminId, "SUSPEND_USER", userId, request.getReason());

        log.info("User suspended successfully: {}", userId);
        return mapToUserResponse(user);
    }
    @Override
    @Transactional
    public UserResponse unsuspendUser(UUID userId, UUID adminId, String note) {
        log.info("Unsuspending user: {} by admin: {}", userId, adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new IllegalStateException("User is not suspended");
        }

        // Update user status
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        // Unfreeze all user wallets
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        for (Wallet wallet : wallets) {
            if (wallet.getStatus() == WalletStatus.FROZEN) {
                wallet.setStatus(WalletStatus.ACTIVE);
                walletRepository.save(wallet);
            }
        }

        // Send notification
        notificationService.sendNotification(
                userId,
                NotificationType.SECURITY_ALERT,
                "Account Reactivated",
                "Your account has been reactivated. You can now access all features.",
                null
        );

        // Log admin action
        logAdminAction(adminId, "UNSUSPEND_USER", userId, note);

        log.info("User unsuspended successfully: {}", userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse lockUser(UUID userId, UUID adminId, AdminActionRequest request) {
        log.info("Locking user: {} by admin: {}, reason: {}", userId, adminId, request.getReason());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalStateException("User is already locked");
        }

        // Update user status
        user.setStatus(UserStatus.LOCKED);
        user = userRepository.save(user);

        // Freeze all user wallets
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        for (Wallet wallet : wallets) {
            wallet.setStatus(WalletStatus.FROZEN);
            walletRepository.save(wallet);
        }

        // Send notification
        notificationService.sendNotification(
                userId,
                NotificationType.ACCOUNT_LOCKED,
                "Account Locked",
                "Your account has been locked due to security reasons. Reason: " +
                        request.getReason() + ". Please contact support immediately.",
                null
        );

        // Log admin action
        logAdminAction(adminId, "LOCK_USER", userId, request.getReason());

        log.info("User locked successfully: {}", userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse unlockUser(UUID userId, UUID adminId, String note) {
        log.info("Unlocking user: {} by admin: {}", userId, adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.LOCKED) {
            throw new IllegalStateException("User is not locked");
        }

        // Update user status
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        // Unfreeze all user wallets
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        for (Wallet wallet : wallets) {
            if (wallet.getStatus() == WalletStatus.FROZEN) {
                wallet.setStatus(WalletStatus.ACTIVE);
                walletRepository.save(wallet);
            }
        }

        // Send notification
        notificationService.sendNotification(
                userId,
                NotificationType.SECURITY_ALERT,
                "Account Unlocked",
                "Your account has been unlocked. You can now login and access your wallet.",
                null
        );

        // Log admin action
        logAdminAction(adminId, "UNLOCK_USER", userId, note);

        log.info("User unlocked successfully: {}", userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId, UUID adminId, AdminActionRequest request) {
        log.info("Deleting user: {} by admin: {}, reason: {}", userId, adminId, request.getReason());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if user has balance
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        BigDecimal totalBalance = wallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot delete user with non-zero balance. Current balance: " + totalBalance);
        }

        // Check for pending transactions
        long pendingTransactions = transactionRepository
                .countByUserIdAndStatus(userId, TransactionStatus.PENDING);

        if (pendingTransactions > 0) {
            throw new IllegalStateException("Cannot delete user with pending transactions");
        }

        // Soft delete - mark as closed instead of hard delete
        user.setStatus(UserStatus.CLOSED);
        userRepository.save(user);

        // Close all wallets
        for (Wallet wallet : wallets) {
            wallet.setStatus(WalletStatus.CLOSED);
            walletRepository.save(wallet);
        }

        // Log admin action
        logAdminAction(adminId, "DELETE_USER", userId, request.getReason());

        log.info("User deleted (soft delete) successfully: {}", userId);
    }

    /**
     * TRANSACTION MANAGEMENT
     * @param reference
     * @param userId
     * @param status
     * @param type
     * @param startDate
     * @param endDate
     * @param pageable
     * @return
     */

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> searchTransactions(
            String reference,
            UUID userId,
            TransactionStatus status,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.info("Searching transactions: ref={}, user={}, status={}, type={}, start={}, end={}",
                reference, userId, status, type, startDate, endDate);

        if (reference != null && !reference.isEmpty()) {
            return transactionRepository.findByReferenceContaining(reference, pageable)
                    .map(this::mapToTransactionResponse);
        }

        if (userId != null && status != null && startDate != null && endDate != null) {
            return transactionRepository.findByUserIdAndStatusAndDateRange(
                            userId, status, startDate, endDate, pageable)
                    .map(this::mapToTransactionResponse);
        }

        if (userId != null) {
            return transactionRepository.findByUserId(userId, pageable)
                    .map(this::mapToTransactionResponse);
        }

        if (status != null && type != null) {
            return transactionRepository.findByStatusAndType(status, type, pageable)
                    .map(this::mapToTransactionResponse);
        }

        if (startDate != null && endDate != null) {
            return transactionRepository.findByCreatedAtBetween(startDate, endDate, pageable)
                    .map(this::mapToTransactionResponse);
        }

        return transactionRepository.findAll(pageable)
                .map(this::mapToTransactionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getTransactionDetails(String reference) {
        log.info("Fetching transaction details: {}", reference);

        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Get ledger entries for this transaction
        List<LedgerEntry> ledgerEntries = ledgerRepository
                .findByTransactionReference(reference);

        return TransactionDetailResponse.builder()
                .transaction(mapToTransactionResponse(transaction))
                .ledgerEntries(ledgerEntries.stream()
                        .map(this::mapToLedgerResponse)
                        .collect(Collectors.toList()))
                .build();
    }
    @Override
    @Transactional
    public TransactionResponse reverseTransaction(String reference, UUID adminId,
                                                  AdminActionRequest request) {
        log.info("Reversing transaction: {} by admin: {}, reason: {}",
                reference, adminId, request.getReason());

        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new IllegalStateException("Only successful transactions can be reversed");
        }

        if (transaction.getType() == TransactionType.REVERSAL) {
            throw new IllegalStateException("Cannot reverse a reversal transaction");
        }

        // Create reversal transaction
        transaction.setStatus(TransactionStatus.REVERSED);
        transaction = transactionRepository.save(transaction);

        // Log admin action
        logAdminAction(adminId, "REVERSE_TRANSACTION", reference, request.getReason());

        log.info("Transaction reversed successfully: {}", reference);
        return mapToTransactionResponse(transaction);
    }

    /**
     * WALLET MANAGEMENT
     * @param walletId
     * @param adminId
     * @param request
     * @return
     */
    @Override
    @Transactional
    public WalletResponse freezeWallet(UUID walletId, UUID adminId, AdminActionRequest request) {
        log.info("Freezing wallet: {} by admin: {}, reason: {}",
                walletId, adminId, request.getReason());

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setStatus(WalletStatus.FROZEN);
        wallet = walletRepository.save(wallet);

        // Notify user
        notificationService.sendNotification(
                wallet.getUser().getId(),
                NotificationType.SECURITY_ALERT,
                "Wallet Frozen",
                "Your " + wallet.getCurrency() + " wallet has been frozen. Reason: " +
                        request.getReason(),
                walletId.toString()
        );

        // Log admin action
        logAdminAction(adminId, "FREEZE_WALLET", walletId, request.getReason());

        log.info("Wallet frozen successfully: {}", walletId);
        return mapToWalletResponse(wallet);
    }
    @Override
    @Transactional
    public WalletResponse unfreezeWallet(UUID walletId, UUID adminId, String note) {
        log.info("Unfreezing wallet: {} by admin: {}", walletId, adminId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setStatus(WalletStatus.ACTIVE);
        wallet = walletRepository.save(wallet);

        // Notify user
        notificationService.sendNotification(
                wallet.getUser().getId(),
                NotificationType.SECURITY_ALERT,
                "Wallet Unfrozen",
                "Your " + wallet.getCurrency() + " wallet has been unfrozen and is now active.",
                walletId.toString()
        );

        // Log admin action
        logAdminAction(adminId, "UNFREEZE_WALLET", walletId, note);

        log.info("Wallet unfrozen successfully: {}", walletId);
        return mapToWalletResponse(wallet);
    }

    /**
     * REPORTING & ANALYTICS
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public TransactionReportResponse generateTransactionReport(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("Generating transaction report: {} to {}", startDate, endDate);

        List<Transaction> transactions = transactionRepository
                .findByCreatedAtBetween(startDate, endDate);

        long totalCount = transactions.size();
        BigDecimal totalVolume = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<TransactionType, Long> countByType = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.counting()
                ));

        Map<TransactionStatus, Long> countByStatus = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getStatus,
                        Collectors.counting()
                ));

        Map<Currency, BigDecimal> volumeByCurrency = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .collect(Collectors.groupingBy(
                        Transaction::getCurrency,
                        Collectors.mapping(
                                Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        return TransactionReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalCount(totalCount)
                .totalVolume(totalVolume)
                .countByType(countByType)
                .countByStatus(countByStatus)
                .volumeByCurrency(volumeByCurrency)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * HELPER METHODS
     * @param user
     * @return
     */


    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .profileImageUrl(user.getProfileImageUrl())
                .kycStatus(user.getKycStatus())
                .mfaEnabled(user.getMfaEnabled())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .availableBalance(wallet.getAvailableBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
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

    private LedgerResponse mapToLedgerResponse(LedgerEntry entry) {
        return LedgerResponse.builder()
                .id(entry.getId())
                .walletId(entry.getWallet().getId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .balanceBefore(entry.getBalanceBefore())
                .balanceAfter(entry.getBalanceAfter())
                .transactionReference(entry.getTransactionReference())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
    private void logAdminAction(UUID adminId, String action, Object targetId, String reason) {
        log.info("ADMIN_ACTION: admin={}, action={}, target={}, reason={}",
                adminId, action, targetId, reason);
        // create an audit table In phase 2 production, save to audit_logs table
    }
}
