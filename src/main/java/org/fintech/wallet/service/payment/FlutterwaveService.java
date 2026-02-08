package org.fintech.wallet.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.request.InitiatePaymentRequest;
import org.fintech.wallet.dto.response.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlutterwaveService {

    private final RestTemplate restTemplate;

    @Value("${payment.flutterwave.secret-key}")
    private String flutterwaveSecretKey;

    @Value("${payment.flutterwave.base-url:https://api.flutterwave.com/v3}")
    private String flutterwaveBaseUrl;

    public PaymentResponse initiateDeposit(InitiatePaymentRequest request) {
        log.info("Initiating Flutterwave deposit for user: {}", request.getUserId());

        String url = flutterwaveBaseUrl + "/payments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(flutterwaveSecretKey);

        Map<String, Object> body = new HashMap<>();
        body.put("tx_ref", request.getReference());
        body.put("amount", request.getAmount());
        body.put("currency", request.getCurrency());
        body.put("redirect_url", request.getCallbackUrl());
        body.put("payment_options", "card,banktransfer");

        Map<String, String> customer = new HashMap<>();
        customer.put("email", request.getEmail());
        body.put("customer", customer);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("user_id", request.getUserId().toString());
        metadata.put("wallet_id", request.getWalletId().toString());
        body.put("meta", metadata);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");

                return PaymentResponse.builder()
                        .success(true)
                        .reference(request.getReference())
                        .authorizationUrl((String) data.get("link"))
                        .build();
            }

            throw new RuntimeException("Failed to initialize Flutterwave transaction");

        } catch (Exception e) {
            log.error("Flutterwave initialization failed", e);
            throw new RuntimeException("Payment initialization failed: " + e.getMessage());
        }
    }

    public boolean verifyTransaction(String transactionId) {
        log.info("Verifying Flutterwave transaction: {}", transactionId);

        String url = flutterwaveBaseUrl + "/transactions/" + transactionId + "/verify";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(flutterwaveSecretKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                String status = (String) data.get("status");

                return "successful".equalsIgnoreCase(status);
            }

            return false;

        } catch (Exception e) {
            log.error("Flutterwave verification failed", e);
            return false;
        }
    }
}
