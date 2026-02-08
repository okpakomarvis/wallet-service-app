package org.fintech.wallet.domain.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum KycLevel {

    /**
     * No KYC completed
     * Very small limits, basic usage only
     */
    NONE(
            "No KYC",
            BigDecimal.valueOf(10_000),     // per transaction
            BigDecimal.valueOf(50_000),     // daily limit
            false
    ),

    /**
     * Basic verification
     * Email + phone / minimal info
     */
    TIER_1(
            "Basic Verification",
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(200_000),
            false
    ),

    /**
     * Enhanced verification
     * Government ID + selfie
     */
    TIER_2(
            "Enhanced Verification",
            BigDecimal.valueOf(500_000),
            BigDecimal.valueOf(2_000_000),
            false
    ),

    /**
     * Full verification
     * Unlimited transactions
     */
    TIER_3(
            "Full Verification",
            null,   // unlimited
            null,   // unlimited
            true
    );

    private final String displayName;
    private final BigDecimal perTransactionLimit;
    private final BigDecimal dailyTransactionLimit;
    private final boolean unlimited;

    KycLevel(
            String displayName,
            BigDecimal perTransactionLimit,
            BigDecimal dailyTransactionLimit,
            boolean unlimited
    ) {
        this.displayName = displayName;
        this.perTransactionLimit = perTransactionLimit;
        this.dailyTransactionLimit = dailyTransactionLimit;
        this.unlimited = unlimited;
    }

    public boolean isUnlimited() {
        return unlimited;
    }
}

