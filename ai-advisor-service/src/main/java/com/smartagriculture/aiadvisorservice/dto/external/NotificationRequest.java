package com.smartagriculture.aiadvisorservice.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String farmerId;
    private String title;
    private String message;
    private String type;     // WEATHER_ALERT, CROP_ADVISORY, PEST_WARNING, IRRIGATION_REMINDER, SYSTEM, OTHER
    private String channel;  // EMAIL, SMS, PUSH, IN_APP
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
}
