package org.fintech.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsResponse {
    private Long totalNotifications;
    private Long unreadCount;
    private Long readCount;
    private Long todayCount;
    private Long thisWeekCount;
    private Map<NotificationType, Long> countByType;
    private Map<NotificationPriority, Long> countByPriority;
    private LocalDateTime lastNotificationAt;
}
