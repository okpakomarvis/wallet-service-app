package org.fintech.wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fintech.wallet.dto.request.CreateWalletRequest;
import org.fintech.wallet.dto.response.ApiResponse;
import org.fintech.wallet.dto.response.WalletResponse;
import org.fintech.wallet.security.CurrentUser;
import org.fintech.wallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Wallet management APIs")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @Operation(
            summary = "Create wallet",
            description = "Create a new wallet for the authenticated user"
    )
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody CreateWalletRequest request) {

        WalletResponse wallet = walletService.createWallet(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", wallet));
    }

    @Operation(
            summary = "Get my wallets",
            description = "Retrieve all wallets owned by the authenticated user"
    )
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getMyWallets(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        List<WalletResponse> wallets = walletService.getUserWallets(userId);
        return ResponseEntity.ok(ApiResponse.success(wallets));
    }

    @Operation(
            summary = "Get wallet by number",
            description = "Retrieve wallet details using wallet number"
    )
    @GetMapping("/{walletNumber}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "Wallet number") @PathVariable String walletNumber) {

        WalletResponse wallet = walletService.getWalletByNumber(walletNumber);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @Operation(
            summary = "Freeze wallet (Admin)",
            description = "Freeze a wallet to prevent transactions"
    )
    @PutMapping("/{walletId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> freezeWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId) {

        walletService.freezeWallet(walletId);
        return ResponseEntity.ok(ApiResponse.success("Wallet frozen successfully", null));
    }

    @Operation(
            summary = "Unfreeze wallet (Admin)",
            description = "Unfreeze a previously frozen wallet"
    )
    @PutMapping("/{walletId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unfreezeWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId) {

        walletService.unfreezeWallet(walletId);
        return ResponseEntity.ok(ApiResponse.success("Wallet unfrozen successfully", null));
    }
}