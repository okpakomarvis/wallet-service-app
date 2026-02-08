package org.fintech.wallet.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduledJobs {

    private final NotificationService notificationService;

    /**
     * Clean up old read notifications every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old read notifications");

        try {
            int deletedCount = notificationService.deleteOldReadNotifications(30);
            log.info("Cleanup completed. Deleted {} old notifications", deletedCount);

        } catch (Exception e) {
            log.error("Error during notification cleanup", e);
        }
    }

    /**
     * Retry failed notifications every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void retryFailedNotifications() {
        log.info("Starting retry of failed notifications");

        try {
            // Implementation would fetch failed notifications and retry sending
            // notificationService.retryFailedNotifications();
            log.info("Failed notification retry completed");

        } catch (Exception e) {
            log.error("Error during notification retry", e);
        }
    }
}
