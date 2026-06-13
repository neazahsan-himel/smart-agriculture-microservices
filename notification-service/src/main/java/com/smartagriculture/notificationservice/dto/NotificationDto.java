package com.smartagriculture.notificationservice.dto;

import com.smartagriculture.notificationservice.entity.Notification;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class NotificationDto {

    // ── Create / Update Request ───────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Farmer ID is required")
        private String farmerId;

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        private String title;

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        private String message;

        @NotNull(message = "Notification type is required")
        private Notification.NotificationType type;

        @NotNull(message = "Notification channel is required")
        private Notification.NotificationChannel channel;

        private Notification.NotificationPriority priority;
    }

    // ── API Response ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private String id;
        private String farmerId;
        private String title;
        private String message;
        private Notification.NotificationType    type;
        private Notification.NotificationChannel channel;
        private Notification.NotificationStatus  status;
        private Notification.NotificationPriority priority;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}