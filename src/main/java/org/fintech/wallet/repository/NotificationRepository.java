package org.fintech.wallet.repository;

import org.fintech.wallet.domain.entity.Notification;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Find notifications by user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Find unread notifications
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Count unread notifications
    long countByUserIdAndIsRead(UUID userId, Boolean isRead);

    // Find by type
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            UUID userId,
            NotificationType type,
            Pageable pageable
    );

    // Find by priority
    Page<Notification> findByUserIdAndPriorityOrderByCreatedAtDesc(
            UUID userId,
            NotificationPriority priority,
            Pageable pageable
    );

    // Find by date range
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "AND n.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Find unsent notifications
    @Query("SELECT n FROM Notification n WHERE n.isSent = false " +
            "AND n.retryCount < 3 ORDER BY n.priority DESC, n.createdAt ASC")
    List<Notification> findUnsentNotifications(Pageable pageable);

    // Find failed notifications for retry
    @Query("SELECT n FROM Notification n WHERE n.isSent = false " +
            "AND n.retryCount > 0 AND n.retryCount < 3 " +
            "ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotificationsForRetry(Pageable pageable);

    // Mark all as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);

    // Delete old read notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true " +
            "AND n.createdAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Statistics
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.user.id = :userId " +
            "GROUP BY n.type")
    List<Object[]> countByTypeForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId " +
            "AND n.createdAt > :since")
    long countByUserIdSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
