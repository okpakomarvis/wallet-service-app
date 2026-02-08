package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${app.sms.provider:TWILIO}")
    private String smsProvider;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio.from-number:}")
    private String twilioFromNumber;

    @Async
    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS disabled. Would send to: {}, message: {}", phoneNumber, message);
            return;
        }

        try {
            log.info("Sending SMS to: {}", phoneNumber);

            if ("TWILIO".equalsIgnoreCase(smsProvider)) {
                sendViaTwilio(phoneNumber, message);
            } else {
                log.warn("Unknown SMS provider: {}", smsProvider);
            }

        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    private void sendViaTwilio(String phoneNumber, String message) {
        String url = String.format(
                "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                twilioAccountSid
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilioAccountSid, twilioAuthToken);

        Map<String, String> body = new HashMap<>();
        body.put("To", phoneNumber);
        body.put("From", twilioFromNumber);
        body.put("Body", message);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("SMS sent successfully via Twilio to: {}", phoneNumber);
            } else {
                log.error("Failed to send SMS via Twilio. Status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error sending SMS via Twilio", e);
            throw e;
        }
    }

    public void sendOtp(String phoneNumber, String otp) {
        String message = String.format(
                "Your verification code is: %s. Valid for 5 minutes. Do not share this code.",
                otp
        );
        sendSms(phoneNumber, message);
    }

    public void sendTransactionAlert(String phoneNumber, String transactionType, String amount) {
        String message = String.format(
                "Wallet Alert: %s of %s completed successfully. Thank you!",
                transactionType, amount
        );
        sendSms(phoneNumber, message);
    }
}
