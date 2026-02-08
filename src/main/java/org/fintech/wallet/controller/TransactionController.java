package org.fintech.wallet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.InvalidTransactionException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fintech.wallet.dto.request.DepositRequest;
import org.fintech.wallet.dto.request.TransferRequest;
import org.fintech.wallet.dto.request.WithdrawalRequest;
import org.fintech.wallet.dto.response.ApiResponse;
import org.fintech.wallet.dto.response.TransactionResponse;
import org.fintech.wallet.security.CurrentUser;
import org.fintech.wallet.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "User transaction operations")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Transfer funds",
            description = "Transfer funds from one wallet to another"
    )
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody TransferRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest)
            throws InvalidTransactionException {

        request.setIpAddress(getClientIp(httpRequest));
        TransactionResponse transaction = transactionService.transfer(request, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Transfer successful", transaction)
        );
    }

    @Operation(
            summary = "Deposit funds",
            description = "Deposit funds into a wallet via payment gateway"
    )
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody DepositRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

        TransactionResponse deposit = transactionService.deposit(
                request.getWalletId(),
                request.getAmount(),
                request.getPaymentReference(),
                request.getGateway()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Deposit successful", deposit)
        );
    }

    @Operation(
            summary = "Withdraw funds",
            description = "Initiate withdrawal from a wallet to a bank account"
    )
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody WithdrawalRequest request) {

        TransactionResponse transaction = transactionService.withdraw(
                request.getWalletId(),
                request.getAmount(),
                request.getBankAccount()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Withdrawal initiated", transaction)
        );
    }

    @Operation(
            summary = "Get my transactions",
            description = "Retrieve paginated transactions for the authenticated user"
    )
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getMyTransactions(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(hidden = true) Pageable pageable) {

        Page<TransactionResponse> transactions =
                transactionService.getUserTransactions(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @Operation(
            summary = "Get transaction by reference",
            description = "Retrieve transaction details using transaction reference"
    )
    @GetMapping("/{reference}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @Parameter(description = "Transaction reference") @PathVariable String reference) {

        TransactionResponse transaction =
                transactionService.getTransactionByReference(reference);

        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}