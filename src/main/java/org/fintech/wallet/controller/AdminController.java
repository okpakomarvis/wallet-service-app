package org.fintech.wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.domain.enums.TransactionStatus;
import org.fintech.wallet.domain.enums.TransactionType;
import org.fintech.wallet.domain.enums.UserStatus;
import org.fintech.wallet.dto.request.AdminActionRequest;
import org.fintech.wallet.dto.request.KycReviewRequest;
import org.fintech.wallet.dto.response.*;
import org.fintech.wallet.security.CurrentUser;
import org.fintech.wallet.service.AdminService;
import org.fintech.wallet.service.KycService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final KycService kycService;

    @Operation(
            summary = "Get all users",
            description = "Retrieve a paginated list of all users"
    )
    @GetMapping("/users")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<Page<UserResponse>>> getAllUsers(
            @Parameter(hidden = true) Pageable pageable) {
        Page<UserResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(users));
    }

    @Operation(
            summary = "Get admin dashboard statistics",
            description = "Retrieve high-level statistics for the admin dashboard"
    )
    @GetMapping("/dashboard/stats")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(stats));
    }

    @Operation(
            summary = "Search users",
            description = "Search users by keyword, status, and KYC status"
    )
    @GetMapping("/users/search")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<Page<UserResponse>>> getAllUsers(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String search,
            @Parameter(description = "User status") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "KYC status") @RequestParam(required = false) KycStatus kycStatus,
            @Parameter(hidden = true) Pageable pageable) {

        Page<UserResponse> users = adminService.searchUsers(search, status, kycStatus, pageable);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(users));
    }

    @Operation(
            summary = "Get user details",
            description = "Retrieve detailed information about a specific user"
    )
    @GetMapping("/users/{userId}")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<AdminUserDetailResponse>> getUserDetails(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        AdminUserDetailResponse details = adminService.getUserDetails(userId);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(details));
    }

    @Operation(
            summary = "Suspend user",
            description = "Suspend a user account"
    )
    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<UserResponse>> suspendUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @RequestBody @Valid AdminActionRequest request) {

        UserResponse user = adminService.suspendUser(userId, adminId, request);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("User suspended successfully", user));
    }

    @Operation(
            summary = "Unsuspend user",
            description = "Remove suspension from a user account"
    )
    @PutMapping("/users/{userId}/unsuspend")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<UserResponse>> unsuspendUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @Parameter(description = "Admin note") @RequestParam(required = false) String note) {

        UserResponse user = adminService.unsuspendUser(userId, adminId, note);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("User unsuspended successfully", user));
    }

    @Operation(
            summary = "Lock user",
            description = "Lock a user account"
    )
    @PutMapping("/users/{userId}/lock")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<UserResponse>> lockUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @RequestBody @Valid AdminActionRequest request) {

        UserResponse user = adminService.lockUser(userId, adminId, request);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("User locked successfully", user));
    }

    @Operation(
            summary = "Unlock user",
            description = "Unlock a previously locked user account"
    )
    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<UserResponse>> unlockUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @Parameter(description = "Admin note") @RequestParam(required = false) String note) {

        UserResponse user = adminService.unlockUser(userId, adminId, note);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("User unlocked successfully", user));
    }

    @Operation(
            summary = "Delete user",
            description = "Permanently delete a user account"
    )
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @RequestBody @Valid AdminActionRequest request) {

        adminService.deleteUser(userId, adminId, request);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("User deleted successfully", null));
    }

    @Operation(
            summary = "Search transactions",
            description = "Search transactions using filters"
    )
    @GetMapping("/transactions")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<Page<TransactionResponse>>> searchTransactions(
            @Parameter(description = "Transaction reference") @RequestParam(required = false) String reference,
            @Parameter(description = "User ID") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Transaction status") @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Transaction type") @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Start date (ISO)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(hidden = true) Pageable pageable) {

        Page<TransactionResponse> transactions = adminService.searchTransactions(
                reference, userId, status, type, startDate, endDate, pageable
        );
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(transactions));
    }

    @Operation(
            summary = "Get transaction details",
            description = "Retrieve detailed information about a transaction"
    )
    @GetMapping("/transactions/{reference}")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<TransactionDetailResponse>> getTransactionDetails(
            @Parameter(description = "Transaction reference") @PathVariable String reference) {

        TransactionDetailResponse details = adminService.getTransactionDetails(reference);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(details));
    }

    @Operation(
            summary = "Reverse transaction",
            description = "Reverse a completed transaction"
    )
    @PostMapping("/transactions/{reference}/reverse")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<TransactionResponse>> reverseTransaction(
            @Parameter(description = "Transaction reference") @PathVariable String reference,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @RequestBody @Valid AdminActionRequest request) {

        TransactionResponse transaction = adminService.reverseTransaction(reference, adminId, request);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("Transaction reversed successfully", transaction));
    }

    @Operation(
            summary = "Freeze wallet",
            description = "Freeze a user wallet"
    )
    @PutMapping("/wallets/{walletId}/freeze")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<WalletResponse>> freezeWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @RequestBody @Valid AdminActionRequest request) {

        WalletResponse wallet = adminService.freezeWallet(walletId, adminId, request);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("Wallet frozen successfully", wallet));
    }

    @Operation(
            summary = "Unfreeze wallet",
            description = "Unfreeze a frozen wallet"
    )
    @PutMapping("/wallets/{walletId}/unfreeze")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<WalletResponse>> unfreezeWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @Parameter(description = "Admin note") @RequestParam(required = false) String note) {

        WalletResponse wallet = adminService.unfreezeWallet(walletId, adminId, note);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("Wallet unfrozen successfully", wallet));
    }

    @Operation(
            summary = "Get pending KYC",
            description = "Retrieve all pending KYC requests"
    )
    @GetMapping("/kyc/pending")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<Page<KycResponse>>> getPendingKyc(
            @Parameter(hidden = true) Pageable pageable) {
        Page<KycResponse> kyc = kycService.getPendingKyc(pageable);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(kyc));
    }

    @Operation(
            summary = "Approve KYC",
            description = "Approve a KYC request"
    )
    @PutMapping("/kyc/{kycId}/approve")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<KycResponse>> approveKyc(
            @Parameter(description = "KYC ID") @PathVariable UUID kycId,
            @Parameter(hidden = true) @CurrentUser UUID adminId) {

        KycResponse kyc = kycService.approveKyc(kycId, adminId);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("KYC approved successfully", kyc));
    }

    @Operation(
            summary = "Reject KYC",
            description = "Reject a KYC request"
    )
    @PutMapping("/kyc/{kycId}/reject")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<KycResponse>> rejectKyc(
            @Parameter(description = "KYC ID") @PathVariable UUID kycId,
            @Parameter(hidden = true) @CurrentUser UUID adminId,
            @RequestBody @Valid KycReviewRequest request) {

        KycResponse kyc = kycService.rejectKyc(kycId, adminId, request.getReason());
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success("KYC rejected", kyc));
    }

    @Operation(
            summary = "Generate transaction report",
            description = "Generate transaction report for a date range"
    )
    @GetMapping("/reports/transactions")
    public ResponseEntity<org.fintech.wallet.dto.response.ApiResponse<TransactionReportResponse>> generateTransactionReport(
            @Parameter(description = "Start date (ISO)") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO)") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        TransactionReportResponse report = adminService.generateTransactionReport(startDate, endDate);
        return ResponseEntity.ok(org.fintech.wallet.dto.response.ApiResponse.success(report));
    }
}
