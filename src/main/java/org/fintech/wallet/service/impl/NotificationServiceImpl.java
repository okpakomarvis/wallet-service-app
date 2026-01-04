package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.Notification;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.dto.request.BulkNotificationRequest;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.dto.response.NotificationResponse;
import org.fintech.wallet.dto.response.NotificationStatsResponse;
import org.fintech.wallet.repository.NotificationRepository;
import org.fintech.wallet.repository.UserRepository;
import org.fintech.wallet.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.fintech.wallet.domain.enums.NotificationChannel.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    // private final EmailService emailService;
    // private final SmsService smsService;
    // private final PushNotificationService pushService;

    // =========================================================================
    // SEND NOTIFICATIONS
    // =========================================================================
    @Override
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Sending notification: user={}, type={}, channel={}",
                request.getUserId(), request.getType(), request.getChannel());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String metadataJson = null;
        if (request.getMetadata() != null) {
            metadataJson = objectMapper.writeValueAsString(request.getMetadata());
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .referenceId(request.getReferenceId())
                .channel(request.getChannel())
                .priority(request.getPriority())
                .isRead(false)
                .isSent(false)
                .metadata(metadataJson)
                .retryCount(0)
                .build();

        notification = notificationRepository.save(notification);

        // Send via appropriate channel(s) asynchronously
        sendViaChannel(notification, user);

        log.info("Notification created: id={}", notification.getId());
        return mapToResponse(notification);
    }
    @Override
    @Transactional
    public NotificationResponse sendNotification(UUID userId, NotificationType type,
                                                 String title, String message,
                                                 String referenceId) {
        return sendNotification(SendNotificationRequest.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .channel(NotificationChannel.ALL)
                .priority(NotificationPriority.MEDIUM)
                .build());
    }
    @Override
    @Transactional
    public List<NotificationResponse> sendBulkNotification(BulkNotificationRequest request) {
        log.info("Sending bulk notification to {} users", request.getUserIds().size());

        List<NotificationResponse> responses = new ArrayList<>();

        for (UUID userId : request.getUserIds()) {
            try {
                NotificationResponse response = sendNotification(
                        SendNotificationRequest.builder()
                                .userId(userId)
                                .type(request.getType())
                                .title(request.getTitle())
                                .message(request.getMessage())
                                .channel(request.getChannel())
                                .priority(request.getPriority())
                                .build()
                );
                responses.add(response);
            } catch (Exception e) {
                log.error("Failed to send notification to user: {}", userId, e);
            }
        }

        log.info("Bulk notification completed: sent {} of {} notifications",
                responses.size(), request.getUserIds().size());

        return responses;
    }

    @Async
    protected void sendViaChannel(Notification notification, User user) {
        try {
            switch (notification.getChannel()) {
                case IN_APP:
                    // Already saved in database
                    markAsSent(notification.getId());
                    break;

                case EMAIL:
                    sendEmailNotification(notification, user);
                    break;

                case SMS:
                    sendSmsNotification(notification, user);
                    break;

                case PUSH:
                    sendPushNotification(notification, user);
                    break;

                case ALL:
                    sendEmailNotification(notification, user);
                    sendSmsNotification(notification, user);
                    sendPushNotification(notification, user);
                    markAsSent(notification.getId());
                    break;
            }

            // Publish to Kafka for analytics
            publishNotificationEvent(notification);

        } catch (Exception e) {
            log.error("Failed to send notification via channel: {}",
                    notification.getChannel(), e);
            incrementRetryCount(notification.getId(), e.getMessage());
        }
    }

    private void sendEmailNotification(Notification notification, User user) {
        log.info("Sending email notification to: {}", user.getEmail());
        // emailService.sendEmail(user.getEmail(), notification.getTitle(), notification.getMessage());
        markAsSent(notification.getId());
    }

    private void sendSmsNotification(Notification notification, User user) {
        if (user.getPhoneNumber() != null) {
            log.info("Sending SMS notification to: {}", user.getPhoneNumber());
            // smsService.sendSms(user.getPhoneNumber(), notification.getMessage());
            markAsSent(notification.getId());
        }
    }

    private void sendPushNotification(Notification notification, User user) {
        log.info("Sending push notification to user: {}", user.getId());
        // pushService.sendPushNotification(user.getId(), notification.getTitle(), notification.getMessage());
        markAsSent(notification.getId());
    }

    /**
     *
     * @param userId
     * @param pageable
     * @return
     */
    // READ NOTIFICATIONS


    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByType(UUID userId,
                                                             NotificationType type,
                                                             Pageable pageable) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatsResponse getNotificationStats(UUID userId) {
        log.info("Fetching notification statistics for user: {}", userId);

        long totalNotifications = notificationRepository.count();
        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);
        long readCount = notificationRepository.countByUserIdAndIsRead(userId, true);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = now.minus(7, ChronoUnit.DAYS);

        long todayCount = notificationRepository.countByUserIdSince(userId, startOfDay);
        long thisWeekCount = notificationRepository.countByUserIdSince(userId, startOfWeek);

        // Get count by type
        List<Object[]> typeStats = notificationRepository.countByTypeForUser(userId);
        Map<NotificationType, Long> countByType = typeStats.stream()
                .collect(Collectors.toMap(
                        arr -> (NotificationType) arr[0],
                        arr -> (Long) arr[1]
                ));

        // Get latest notification
        Page<Notification> latest = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1));
        LocalDateTime lastNotificationAt = latest.hasContent() ?
                latest.getContent().get(0).getCreatedAt() : null;

        return NotificationStatsResponse.builder()
                .totalNotifications(totalNotifications)
                .unreadCount(unreadCount)
                .readCount(readCount)
                .todayCount(todayCount)
                .thisWeekCount(thisWeekCount)
                .countByType(countByType)
                .lastNotificationAt(lastNotificationAt)
                .build();
    }

    /**
     *
     * @param notificationId
     */
    // MANAGE NOTIFICATIONS

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification marked as read: {}", notificationId);
        }
    }

    @Override
    @Transactional
    public void markAsUnread(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(false);
        notification.setReadAt(null);
        notificationRepository.save(notification);
        log.info("Notification marked as unread: {}", notificationId);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
        log.info("Marked {} notifications as read for user: {}", count, userId);
        return count;
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
        log.info("Notification deleted: {}", notificationId);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(UUID userId) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged());

        notificationRepository.deleteAll(notifications);
        log.info("All notifications deleted for user: {}", userId);
    }

    @Override
    @Transactional
    public int deleteOldReadNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(daysOld, ChronoUnit.DAYS);
        int count = notificationRepository.deleteOldReadNotifications(cutoffDate);
        log.info("Deleted {} old read notifications (older than {} days)", count, daysOld);
        return count;
    }

    /**
     *
     * @param notificationId
     */
    // HELPER METHODS

    @Transactional
    protected void markAsSent(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    @Transactional
    protected void incrementRetryCount(UUID notificationId, String errorMessage) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setErrorMessage(errorMessage);
            notificationRepository.save(notification);
        });
    }

    private void publishNotificationEvent(Notification notification) {
        try {
            kafkaTemplate.send("notification-events", notification.getId().toString(),
                    mapToResponse(notification));
        } catch (Exception e) {
            log.error("Failed to publish notification event", e);
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        Map<String, Object> metadata = null;
        if (notification.getMetadata() != null) {
            metadata = objectMapper.readValue(notification.getMetadata(), Map.class);
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .channel(notification.getChannel())
                .priority(notification.getPriority())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .metadata(metadata)
                .build();
    }
}