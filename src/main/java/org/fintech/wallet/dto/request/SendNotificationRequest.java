package org.fintech.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;

    @Size(max = 100)
    private String referenceId;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @Builder.Default
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    private Map<String, Object> metadata;
}

