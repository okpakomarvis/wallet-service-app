package org.fintech.wallet.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeNotificationMessage {
    private String event; // NEW_NOTIFICATION, UNREAD_COUNT, DELETED
    private NotificationResponse notification;
    private Long unreadCount;
    private UUID notificationId;
}
