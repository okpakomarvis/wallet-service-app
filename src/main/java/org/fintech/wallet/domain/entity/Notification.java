package org.fintech.wallet.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fintech.wallet.domain.enums.NotificationChannel;
import org.fintech.wallet.domain.enums.NotificationPriority;
import org.fintech.wallet.domain.enums.NotificationType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id, created_at"),
        @Index(name = "idx_notification_read", columnList = "is_read"),
        @Index(name = "idx_notification_type", columnList = "type, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 100)
    private String referenceId; // Transaction ID, KYC ID, Wallet ID, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel; // IN_APP, EMAIL, SMS, PUSH

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority; // LOW, MEDIUM, HIGH, URGENT

    @Column(nullable = false)
    private Boolean isRead = false;

    private LocalDateTime readAt;

    @Column(nullable = false)
    private Boolean isSent = false;

    private LocalDateTime sentAt;

    @Column(length = 1000)
    private String metadata; // JSON string for additional data

    private Integer retryCount = 0;

    @Column(length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

