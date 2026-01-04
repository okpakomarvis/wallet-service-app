package org.fintech.wallet.domain.enums;

public enum NotificationType {
    // Transaction notifications
    TRANSACTION_SUCCESS,
    TRANSACTION_FAILED,
    TRANSACTION_PENDING,
    TRANSACTION_REVERSED,

    // Deposit notifications
    DEPOSIT_SUCCESS,
    DEPOSIT_FAILED,
    DEPOSIT_PENDING,

    // Withdrawal notifications
    WITHDRAWAL_SUCCESS,
    WITHDRAWAL_FAILED,
    WITHDRAWAL_PENDING,

    // Transfer notifications
    TRANSFER_SENT,
    TRANSFER_RECEIVED,
    TRANSFER_FAILED,

    // KYC notifications
    KYC_SUBMITTED,
    KYC_APPROVED,
    KYC_REJECTED,
    KYC_EXPIRED,
    KYC_DOCUMENTS_REQUIRED,

    // Security notifications
    SECURITY_ALERT,
    LOGIN_ATTEMPT,
    PASSWORD_CHANGED,
    PIN_CHANGED,
    MFA_ENABLED,
    MFA_DISABLED,
    SUSPICIOUS_ACTIVITY,

    // Account notifications
    ACCOUNT_CREATED,
    ACCOUNT_VERIFIED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    ACCOUNT_CLOSED,

    // Wallet notifications
    WALLET_CREATED,
    WALLET_FROZEN,
    WALLET_UNFROZEN,
    WALLET_LOW_BALANCE,
    WALLET_LIMIT_REACHED,

    // System notifications
    SYSTEM_MAINTENANCE,
    SYSTEM_UPDATE,
    PROMOTIONAL,
    ANNOUNCEMENT
}
