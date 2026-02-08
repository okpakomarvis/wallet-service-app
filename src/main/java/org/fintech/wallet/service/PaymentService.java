package org.fintech.wallet.service;

import org.fintech.wallet.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    void handlePaymentWebhook(String gateway, String reference, String status);
    PaymentResponse initiateDeposit(UUID walletId, BigDecimal amount,
                                    String email, String gateway);
}
