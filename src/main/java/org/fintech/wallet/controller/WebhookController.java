package org.fintech.wallet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Payment gateway webhook endpoints")
public class WebhookController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Paystack payment webhook",
            description = "Receive and process payment status updates from Paystack"
    )
    @PostMapping("/payment/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(
            @Parameter(description = "Paystack webhook payload")
            @RequestBody Map<String, Object> payload) {

        log.info("Received Paystack webhook");

        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        String reference = (String) data.get("reference");
        String status = (String) data.get("status");

        paymentService.handlePaymentWebhook("PAYSTACK", reference, status);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Flutterwave payment webhook",
            description = "Receive and process payment status updates from Flutterwave"
    )
    @PostMapping("/payment/flutterwave")
    public ResponseEntity<Void> handleFlutterwaveWebhook(
            @Parameter(description = "Flutterwave webhook payload")
            @RequestBody Map<String, Object> payload) {

        log.info("Received Flutterwave webhook");

        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        String reference = (String) data.get("tx_ref");
        String status = (String) data.get("status");

        paymentService.handlePaymentWebhook("FLUTTERWAVE", reference, status);

        return ResponseEntity.ok().build();
    }
}
