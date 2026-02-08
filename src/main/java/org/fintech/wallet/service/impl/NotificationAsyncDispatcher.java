package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.Notification;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.dto.event.NotificationEvent;
import org.fintech.wallet.kafka.KafkaProducerService;
import org.fintech.wallet.repository.NotificationRepository;
import org.fintech.wallet.repository.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAsyncDispatcher {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;

    @Async
    @Transactional
    public void dispatch(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) return;

        User user = userRepository.findById(notification.getUser().getId()).orElse(null);
        if (user == null) return;

        try {
            NotificationChannel channel = notification.getChannel();

            // IN_APP means: saved in DB + considered sent
            switch (channel) {
                case IN_APP -> markAsSent(notification);
                case EMAIL -> { sendEmail(notification, user); markAsSent(notification); }
                case SMS -> { sendSms(notification, user); markAsSent(notification); }
                case PUSH -> { sendPush(notification, user); markAsSent(notification); }
                case ALL -> {
                    sendEmail(notification, user);
                    sendSms(notification, user);
                    sendPush(notification, user);
                    markAsSent(notification);
                }
            }

            // Publish analytics event
            kafkaProducerService.publishNotificationEvent(NotificationEvent.builder()
                    .notificationId(notification.getId())
                    .userId(notification.getUser().getId())
                    .type(notification.getType().name())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .channel(notification.getChannel().name())
                    .priority(notification.getPriority().name())
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Failed to dispatch notification. id={}", notificationId, e);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    private void sendEmail(Notification notification, User user) {
        log.info("Sending email notification to: {}", user.getEmail());
        // TODO: plug real email service
    }

    private void sendSms(Notification notification, User user) {
        if (user.getPhoneNumber() != null) {
            log.info("Sending SMS notification to: {}", user.getPhoneNumber());
            // TODO: plug real SMS service
        }
    }

    private void sendPush(Notification notification, User user) {
        log.info("Sending push notification to user: {}", user.getId());
        // TODO: plug real push service
    }

    private void markAsSent(Notification notification) {
        if (!Boolean.TRUE.equals(notification.getIsSent())) {
            notification.setIsSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
}
