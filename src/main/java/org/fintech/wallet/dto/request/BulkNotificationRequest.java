package org.fintech.wallet.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {

    @NotNull
    private List<UUID> userIds;

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String message;

    @NotNull
    private NotificationChannel channel;

    @Builder.Default
    private NotificationPriority priority = NotificationPriority.MEDIUM;
}
