package org.fintech.wallet.service;

import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.dto.request.BulkNotificationRequest;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.dto.response.NotificationResponse;
import org.fintech.wallet.dto.response.NotificationStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    NotificationResponse sendNotification(UUID userId, NotificationType type, String title,
                          String message, String referenceId);
    NotificationResponse sendNotification(SendNotificationRequest request);
    List<NotificationResponse> sendBulkNotification(BulkNotificationRequest request);
    Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);
    long getUnreadCount(UUID userId);
    void markAsRead(UUID notificationId);
    int markAllAsRead(UUID userId);
    Page<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable);
    Page<NotificationResponse> getNotificationsByType(UUID userId,
                                                      NotificationType type,
                                                      Pageable pageable);
    NotificationStatsResponse getNotificationStats(UUID userId);
    void markAsUnread(UUID notificationId);
    void deleteNotification(UUID notificationId);
    void deleteAllNotifications(UUID userId);
    int deleteOldReadNotifications(int daysOld);
}
