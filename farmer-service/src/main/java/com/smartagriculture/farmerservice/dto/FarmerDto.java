package com.smartagriculture.farmerservice.dto;

import com.smartagriculture.farmerservice.entity.Farmer;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * FarmerDto is split into nested Request/Response classes.
 *
 * - Request: used for create/update (no id, no audit fields)
 * - Response: returned to clients (includes id, audit, computed fields)
 *
 * This separation ensures clients cannot inject audit fields
 * and API contracts stay stable independently.
 */
public class FarmerDto {

    // ── Create / Update Request ───────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Farmer name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        // E.164 format: +8801XXXXXXXXX
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone number must be in E.164 format (e.g. +8801712345678)")
        private String phoneNumber;

        @Email(message = "Invalid email address")
        private String email;

        // ISO 3166-1 alpha-2
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (e.g. BD, IN, US)")
        private String countryCode;

        private String region;
        private String district;

        @DecimalMin(value = "-90.0",  message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0",   message = "Latitude must be between -90 and 90")
        private Double latitude;

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
        private Double longitude;

        @Positive(message = "Farm size must be a positive number")
        private Double farmSizeHectares;

        private Farmer.FarmType       farmType;
        private Farmer.SoilType       soilType;
        private Farmer.IrrigationMethod irrigationMethod;

        // ISO 639-1 (e.g. en, bn, hi)
        @Size(max = 5)
        private String preferredLanguage;

        private String            timezone;
        private Farmer.DeviceType deviceType;
        private Boolean           aiConsentGiven;
    }

    // ── API Response ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private String id;
        private String name;
        private String phoneNumber;
        private String email;
        private String countryCode;
        private String region;
        private String district;
        private Double latitude;
        private Double longitude;
        private Double farmSizeHectares;

        private Farmer.FarmType         farmType;
        private Farmer.SoilType         soilType;
        private Farmer.IrrigationMethod irrigationMethod;
        private String                  preferredLanguage;
        private String                  timezone;
        private Farmer.DeviceType       deviceType;
        private Boolean                 aiConsentGiven;
        private Farmer.FarmerStatus     status;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}