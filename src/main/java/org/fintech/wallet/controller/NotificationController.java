package org.fintech.wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fintech.wallet.domain.enums.NotificationType;
import org.fintech.wallet.dto.request.BulkNotificationRequest;
import org.fintech.wallet.dto.request.SendNotificationRequest;
import org.fintech.wallet.dto.response.ApiResponse;
import org.fintech.wallet.dto.response.NotificationResponse;
import org.fintech.wallet.dto.response.NotificationStatsResponse;
import org.fintech.wallet.security.CurrentUser;
import org.fintech.wallet.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User and admin notification APIs")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get user notifications",
            description = "Retrieve paginated notifications for the authenticated user"
    )
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(hidden = true) Pageable pageable) {

        Page<NotificationResponse> notifications =
                notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @Operation(
            summary = "Get unread notifications",
            description = "Retrieve unread notifications for the authenticated user"
    )
    @GetMapping("/unread")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUnreadNotifications(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(hidden = true) Pageable pageable) {

        Page<NotificationResponse> notifications =
                notificationService.getUnreadNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @Operation(
            summary = "Get unread notification count",
            description = "Retrieve count of unread notifications"
    )
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @Operation(
            summary = "Get notification statistics",
            description = "Retrieve notification statistics for the authenticated user"
    )
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<NotificationStatsResponse>> getNotificationStats(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        NotificationStatsResponse stats = notificationService.getNotificationStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(
            summary = "Get notifications by type",
            description = "Retrieve notifications filtered by notification type"
    )
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotificationsByType(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "Notification type") @PathVariable NotificationType type,
            @Parameter(hidden = true) Pageable pageable) {

        Page<NotificationResponse> notifications =
                notificationService.getNotificationsByType(userId, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @Operation(
            summary = "Mark notification as read",
            description = "Mark a specific notification as read"
    )
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable UUID notificationId) {

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @Operation(
            summary = "Mark notification as unread",
            description = "Mark a specific notification as unread"
    )
    @PutMapping("/{notificationId}/unread")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> markAsUnread(
            @Parameter(description = "Notification ID") @PathVariable UUID notificationId) {

        notificationService.markAsUnread(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as unread", null));
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Mark all notifications for the authenticated user as read"
    )
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(
                ApiResponse.success(count + " notifications marked as read", count)
        );
    }

    @Operation(
            summary = "Delete notification",
            description = "Delete a specific notification"
    )
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @Parameter(description = "Notification ID") @PathVariable UUID notificationId) {

        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    @Operation(
            summary = "Delete all notifications",
            description = "Delete all notifications for the authenticated user"
    )
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteAllNotifications(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications deleted", null));
    }

    @Operation(
            summary = "Send notification (Admin)",
            description = "Send a notification to a single user (Admin only)"
    )
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        NotificationResponse notification = notificationService.sendNotification(request);
        return ResponseEntity.ok(
                ApiResponse.success("Notification sent successfully", notification)
        );
    }

    @Operation(
            summary = "Send bulk notification (Admin)",
            description = "Send notifications to multiple users (Admin only)"
    )
    @PostMapping("/send-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> sendBulkNotification(
            @Valid @RequestBody BulkNotificationRequest request) {

        List<NotificationResponse> notifications =
                notificationService.sendBulkNotification(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Bulk notification sent to " + notifications.size() + " users",
                        notifications
                )
        );
    }
}
