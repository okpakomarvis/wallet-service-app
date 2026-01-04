package org.fintech.wallet.domain.constant.notification;

import org.fintech.wallet.domain.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationTemplateService {

    public String getTitle(NotificationType type) {
        return switch (type) {
            case TRANSACTION_SUCCESS -> "Transaction Successful";
            case TRANSACTION_FAILED -> "Transaction Failed";
            case DEPOSIT_SUCCESS -> "Deposit Successful";
            case WITHDRAWAL_SUCCESS -> "Withdrawal Successful";
            case TRANSFER_SENT -> "Transfer Sent";
            case TRANSFER_RECEIVED -> "Money Received";
            case KYC_APPROVED -> "KYC Verification Approved";
            case KYC_REJECTED -> "KYC Verification Rejected";
            case SECURITY_ALERT -> "Security Alert";
            case PASSWORD_CHANGED -> "Password Changed";
            case ACCOUNT_SUSPENDED -> "Account Suspended";
            case WALLET_FROZEN -> "Wallet Frozen";
            default -> "Notification";
        };
    }

    public String getMessage(NotificationType type, Map<String, Object> params) {
        return switch (type) {
            case TRANSACTION_SUCCESS -> String.format(
                    "Your transaction of %s has been completed successfully. Reference: %s",
                    params.get("amount"), params.get("reference")
            );
            case DEPOSIT_SUCCESS -> String.format(
                    "Your deposit of %s has been credited to your wallet.",
                    params.get("amount")
            );
            case TRANSFER_RECEIVED -> String.format(
                    "You received %s from %s",
                    params.get("amount"), params.get("sender")
            );
            case KYC_APPROVED ->
                    "Congratulations! Your KYC verification has been approved. " +
                            "You can now enjoy full access to all features.";
            case SECURITY_ALERT -> String.format(
                    "Security alert: %s. If this wasn't you, please contact support immediately.",
                    params.get("message")
            );
            default -> String.format("Notification: %s", params.get("message"));
        };
    }
}