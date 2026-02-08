package org.fintech.wallet.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private UUID notificationId;
    private UUID userId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private String priority;
    private LocalDateTime timestamp;
}
