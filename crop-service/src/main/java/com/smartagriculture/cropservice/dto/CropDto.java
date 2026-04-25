package com.smartagriculture.cropservice.dto;

import com.smartagriculture.cropservice.entity.Crop;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class CropDto {

    // ── Create / Update Request ───────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Crop name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @Size(max = 100, message = "Variety must be at most 100 characters")
        private String variety;

        @Size(max = 500, message = "Description must be at most 500 characters")
        private String description;

        private Crop.CropType     cropType;
        private Crop.GrowingSeason season;
        private Crop.SoilType     idealSoilType;

        @DecimalMin(value = "-50.0", message = "Min temperature must be at least -50°C")
        @DecimalMax(value = "60.0",  message = "Min temperature must be at most 60°C")
        private Double minTemperatureCelsius;

        @DecimalMin(value = "-50.0", message = "Max temperature must be at least -50°C")
        @DecimalMax(value = "60.0",  message = "Max temperature must be at most 60°C")
        private Double maxTemperatureCelsius;

        @PositiveOrZero(message = "Min rainfall must be zero or positive")
        private Double minRainfallMm;

        @PositiveOrZero(message = "Max rainfall must be zero or positive")
        private Double maxRainfallMm;

        @Positive(message = "Growing duration must be a positive number of days")
        private Integer growingDurationDays;

        private Boolean irrigationRequired;

        @Positive(message = "Typical yield must be a positive number")
        private Double typicalYieldPerHectare;

        // ISO 3166-1 alpha-2
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (e.g. BD, IN)")
        private String countryCode;

        private String region;
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
        private String variety;
        private String description;

        private Crop.CropType      cropType;
        private Crop.GrowingSeason season;
        private Crop.SoilType      idealSoilType;

        private Double  minTemperatureCelsius;
        private Double  maxTemperatureCelsius;
        private Double  minRainfallMm;
        private Double  maxRainfallMm;
        private Integer growingDurationDays;

        private Boolean irrigationRequired;
        private Double  typicalYieldPerHectare;

        private String countryCode;
        private String region;

        private Crop.CropStatus status;
        private LocalDateTime   createdAt;
        private LocalDateTime   updatedAt;
    }
}