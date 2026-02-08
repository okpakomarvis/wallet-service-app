package org.fintech.wallet.service.realtime;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.dto.response.NotificationResponse;
import org.fintech.wallet.dto.response.RealtimeNotificationMessage;
import org.fintech.wallet.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public void publishNew(UUID userId, NotificationResponse notification) {
        long unread = notificationRepository.countByUserIdAndIsRead(userId, false);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                RealtimeNotificationMessage.builder()
                        .event("NEW_NOTIFICATION")
                        .notification(notification)
                        .unreadCount(unread)
                        .build()
        );

        publishUnreadCount(userId);
    }

    public void publishUnreadCount(UUID userId) {
        long unread = notificationRepository.countByUserIdAndIsRead(userId, false);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications/unread-count",
                RealtimeNotificationMessage.builder()
                        .event("UNREAD_COUNT")
                        .unreadCount(unread)
                        .build()
        );
    }

    public void publishDeleted(UUID userId, UUID notificationId) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                RealtimeNotificationMessage.builder()
                        .event("DELETED")
                        .notificationId(notificationId)
                        .build()
        );
    }
}
