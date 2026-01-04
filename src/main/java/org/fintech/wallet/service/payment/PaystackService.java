package org.fintech.wallet.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.request.InitiatePaymentRequest;
import org.fintech.wallet.dto.response.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackService {

    private final RestTemplate restTemplate;

    @Value("${payment.paystack.secret-key}")
    private String paystackSecretKey;

    @Value("${payment.paystack.base-url:https://api.paystack.co}")
    private String paystackBaseUrl;

    public PaymentResponse initiateDeposit(InitiatePaymentRequest request) {
        log.info("Initiating Paystack deposit for user: {}", request.getUserId());

        String url = paystackBaseUrl + "/transaction/initialize";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(paystackSecretKey);

        Map<String, Object> body = new HashMap<>();
        body.put("email", request.getEmail());
        body.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue()); // Convert to kobo
        body.put("currency", request.getCurrency());
        body.put("reference", request.getReference());
        body.put("callback_url", request.getCallbackUrl());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("user_id", request.getUserId().toString());
        metadata.put("wallet_id", request.getWalletId().toString());
        body.put("metadata", metadata);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");

                return PaymentResponse.builder()
                        .success(true)
                        .reference(request.getReference())
                        .authorizationUrl((String) data.get("authorization_url"))
                        .accessCode((String) data.get("access_code"))
                        .build();
            }

            throw new RuntimeException("Failed to initialize Paystack transaction");

        } catch (Exception e) {
            log.error("Paystack initialization failed", e);
            throw new RuntimeException("Payment initialization failed: " + e.getMessage());
        }
    }

    public boolean verifyTransaction(String reference) {
        log.info("Verifying Paystack transaction: {}", reference);

        String url = paystackBaseUrl + "/transaction/verify/" + reference;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                String status = (String) data.get("status");

                return "success".equalsIgnoreCase(status);
            }

            return false;

        } catch (Exception e) {
            log.error("Paystack verification failed", e);
            return false;
        }
    }
}
