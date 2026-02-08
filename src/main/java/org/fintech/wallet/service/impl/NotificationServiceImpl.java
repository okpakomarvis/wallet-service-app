package org.fintech.wallet.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.fintech.wallet.service.realtime.NotificationRealtimePublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final NotificationAsyncDispatcher asyncDispatcher;
    private final NotificationRealtimePublisher realtimePublisher;

    /**
     * SEND NOTIFICATIONS
     * @param request
     * @return
     */

    @Override
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Sending notification: user={}, type={}, channel={}",
                request.getUserId(), request.getType(), request.getChannel());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // DEDUPE (important for Kafka retries): if same type+referenceId already exists, return latest
        if (request.getReferenceId() != null && !request.getReferenceId().isBlank()) {
            boolean exists = notificationRepository.existsByUserIdAndTypeAndReferenceId(
                    request.getUserId(), request.getType(), request.getReferenceId()
            );
            if (exists) {
                Notification existing = notificationRepository
                        .findFirstByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(
                                request.getUserId(), request.getType(), request.getReferenceId()
                        )
                        .orElse(null);

                if (existing != null) {
                    log.info("Dedupe hit: returning existing notification id={}", existing.getId());
                    return mapToResponse(existing);
                }
            }
        }

        String metadataJson = null;
        if (request.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            } catch (Exception e) {
                log.warn("Failed to serialize notification metadata. Dropping metadata. user={}, type={}",
                        request.getUserId(), request.getType(), e);
            }
        }

        NotificationChannel channel = request.getChannel() != null ? request.getChannel() : NotificationChannel.ALL;
        NotificationPriority priority = request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM;

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .referenceId(request.getReferenceId())
                .channel(channel)
                .priority(priority)
                .isRead(false)
                .isSent(false)
                .metadata(metadataJson)
                .retryCount(0)
                .build();

        notification = notificationRepository.save(notification);

        // Dispatch external channels asynchronously (IMPORTANT: @Async must be in a different bean)
        asyncDispatcher.dispatch(notification.getId());

        // Realtime push AFTER COMMIT to avoid “ghost notifications”
        final UUID savedNotificationId = notification.getId();
        final UUID savedUserId = user.getId();

        runAfterCommit(() -> {
            try {
                Notification saved = notificationRepository.findById(savedNotificationId).orElse(null);
                if (saved != null) {
                    realtimePublisher.publishNew(savedUserId, mapToResponse(saved));
                }
            } catch (Exception e) {
                log.error("Realtime publish failed for notification={}", savedNotificationId, e);
            }
        });

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
                responses.add(sendNotification(SendNotificationRequest.builder()
                        .userId(userId)
                        .type(request.getType())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .channel(request.getChannel())
                        .priority(request.getPriority())
                        .build()));
            } catch (Exception e) {
                log.error("Failed to send notification to user: {}", userId, e);
            }
        }

        log.info("Bulk notification completed: sent {} of {} notifications",
                responses.size(), request.getUserIds().size());
        return responses;
    }

    /**
     * READ NOTIFICATIONS
     * @param userId
     * @param pageable
     * @return
     */

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
    public Page<NotificationResponse> getNotificationsByType(UUID userId, NotificationType type, Pageable pageable) {
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

        long totalNotifications = notificationRepository.countByUserId(userId);
        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);
        long readCount = notificationRepository.countByUserIdAndIsRead(userId, true);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = now.minus(7, ChronoUnit.DAYS);

        long todayCount = notificationRepository.countByUserIdSince(userId, startOfDay);
        long thisWeekCount = notificationRepository.countByUserIdSince(userId, startOfWeek);

        List<Object[]> typeStats = notificationRepository.countByTypeForUser(userId);
        Map<NotificationType, Long> countByType = typeStats.stream()
                .collect(Collectors.toMap(
                        arr -> (NotificationType) arr[0],
                        arr -> (Long) arr[1]
                ));

        Page<Notification> latest = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1));
        LocalDateTime lastNotificationAt = latest.hasContent()
                ? latest.getContent().get(0).getCreatedAt()
                : null;

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
     * MANAGE NOTIFICATIONS
     * @param notificationId
     */

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);

            UUID userId = notification.getUser().getId();
            runAfterCommit(() -> realtimePublisher.publishUnreadCount(userId));

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

        UUID userId = notification.getUser().getId();
        runAfterCommit(() -> realtimePublisher.publishUnreadCount(userId));

        log.info("Notification marked as unread: {}", notificationId);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());

        runAfterCommit(() -> realtimePublisher.publishUnreadCount(userId));

        log.info("Marked {} notifications as read for user: {}", count, userId);
        return count;
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        UUID userId = notification.getUser().getId();
        notificationRepository.delete(notification);

        runAfterCommit(() -> {
            realtimePublisher.publishDeleted(userId, notificationId);
            realtimePublisher.publishUnreadCount(userId);
        });

        log.info("Notification deleted: {}", notificationId);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(UUID userId) {
        notificationRepository.deleteByUserId(userId);

        runAfterCommit(() -> realtimePublisher.publishUnreadCount(userId));

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
     * MAPPING
     * @param notification
     * @return
     */

    private NotificationResponse mapToResponse(Notification notification) {
        Map<String, Object> metadata = null;
        if (notification.getMetadata() != null) {
            try {
                metadata = objectMapper.readValue(notification.getMetadata(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse notification metadata. id={}", notification.getId(), e);
            }
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

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { task.run(); }
            });
        } else {
            task.run();
        }
    }
}