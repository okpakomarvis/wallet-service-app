package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.request.InitiatePaymentRequest;
import org.fintech.wallet.dto.response.PaymentResponse;
import org.fintech.wallet.service.PaymentService;
import org.fintech.wallet.service.TransactionService;
import org.fintech.wallet.service.payment.FlutterwaveService;
import org.fintech.wallet.service.payment.PaystackService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaystackService paystackService;
    private final FlutterwaveService flutterwaveService;
    private final TransactionService transactionService;

    public PaymentResponse initiateDeposit(UUID walletId, BigDecimal amount,
                                           String email, String gateway) {
        log.info("Initiating deposit: wallet={}, amount={}, gateway={}",
                walletId, amount, gateway);

        String reference = generateReference("DEP");

        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                .userId(UUID.randomUUID()) // Get from context
                .walletId(walletId)
                .amount(amount)
                .currency("NGN")
                .reference(reference)
                .email(email)
                .callbackUrl("https://yourapp.com/api/v1/webhooks/payment")
                .build();

        PaymentResponse response;
        if ("PAYSTACK".equalsIgnoreCase(gateway)) {
            response = paystackService.initiateDeposit(request);
        } else if ("FLUTTERWAVE".equalsIgnoreCase(gateway)) {
            response = flutterwaveService.initiateDeposit(request);
        } else {
            throw new IllegalArgumentException("Unsupported payment gateway: " + gateway);
        }

        return response;
    }

    @Override
    public void handlePaymentWebhook(String gateway, String reference, String status) {
        log.info("Processing webhook: gateway={}, reference={}, status={}",
                gateway, reference, status);

        boolean verified = false;
        if ("PAYSTACK".equalsIgnoreCase(gateway)) {
            verified = paystackService.verifyTransaction(reference);
        } else if ("FLUTTERWAVE".equalsIgnoreCase(gateway)) {
            verified = flutterwaveService.verifyTransaction(reference);
        }

        if (verified && "success".equalsIgnoreCase(status)) {
            // Process successful payment
            // transactionService.completeDeposit(reference);
            log.info("Payment verified and processed: {}", reference);
        } else {
            log.warn("Payment verification failed: {}", reference);
        }
    }

    private String generateReference(String prefix) {
        return String.format("%s%d%06d", prefix, System.currentTimeMillis(),
                (int)(Math.random() * 1000000));
    }
}