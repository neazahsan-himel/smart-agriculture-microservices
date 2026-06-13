package com.smartagriculture.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_farmer",  columnList = "farmerId"),
        @Index(name = "idx_notif_type",    columnList = "type"),
        @Index(name = "idx_notif_status",  columnList = "status"),
        @Index(name = "idx_notif_channel", columnList = "channel"),
        @Index(name = "idx_notif_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    // Cross-service reference — stored as plain String, no FK
    @Column(nullable = false)
    private String farmerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String message;

    // ── Classification ────────────────────────────────────────────────────────

    // WEATHER_ALERT, CROP_ADVISORY, PEST_WARNING, IRRIGATION_REMINDER, SYSTEM, OTHER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // EMAIL, SMS, PUSH, IN_APP
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    // PENDING → SENT / FAILED → READ
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    // LOW, MEDIUM, HIGH, CRITICAL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum NotificationType {
        WEATHER_ALERT, CROP_ADVISORY, PEST_WARNING, IRRIGATION_REMINDER, SYSTEM, OTHER
    }

    public enum NotificationChannel {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, READ
    }

    public enum NotificationPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}