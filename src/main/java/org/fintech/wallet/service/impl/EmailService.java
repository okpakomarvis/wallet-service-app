package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@wallet.com}")
    private String fromEmail;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Async
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("Email disabled. Would send to: {}, subject: {}", to, subject);
            return;
        }

        try {
            log.info("Sending email to: {}", to);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.info("Email disabled. Would send HTML email to: {}", to);
            return;
        }

        try {
            log.info("Sending HTML email to: {}", to);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);

        } catch (MessagingException | jakarta.mail.MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    public void sendTransactionNotification(String to, String transactionType,
                                            String amount, String reference) {
        String subject = "Transaction " + transactionType + " Confirmation";
        String body = String.format(
                "Dear Customer,\n\n" +
                        "Your %s transaction has been processed successfully.\n\n" +
                        "Amount: %s\n" +
                        "Reference: %s\n\n" +
                        "Thank you for using our service.\n\n" +
                        "Best regards,\n" +
                        "Wallet Team",
                transactionType, amount, reference
        );

        sendEmail(to, subject, body);
    }

    public void sendKycApprovalNotification(String to, String userName) {
        String subject = "KYC Verification Approved";
        String body = String.format(
                "Dear %s,\n\n" +
                        "Congratulations! Your KYC verification has been approved.\n\n" +
                        "You can now enjoy full access to all features including:\n" +
                        "- Higher transaction limits\n" +
                        "- Withdrawal to bank accounts\n" +
                        "- All premium features\n\n" +
                        "Thank you for completing your verification.\n\n" +
                        "Best regards,\n" +
                        "Wallet Team",
                userName
        );

        sendEmail(to, subject, body);
    }

    public void sendSecurityAlert(String to, String alertMessage) {
        String subject = "Security Alert - Wallet Account";
        String body = String.format(
                "Dear Customer,\n\n" +
                        "We detected the following security event on your account:\n\n" +
                        "%s\n\n" +
                        "If this was not you, please contact support immediately.\n\n" +
                        "Best regards,\n" +
                        "Wallet Security Team",
                alertMessage
        );

        sendEmail(to, subject, body);
    }
}