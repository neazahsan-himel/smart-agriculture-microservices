package com.smartagriculture.weatherservice.dto;

import com.smartagriculture.weatherservice.entity.WeatherRecord;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class WeatherDto {

    // ── Create / Update Request ───────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (e.g. BD, IN)")
        private String countryCode;

        private String region;

        @DecimalMin(value = "-90.0",  message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0",   message = "Latitude must be between -90 and 90")
        private Double latitude;

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
        private Double longitude;

        @NotNull(message = "Recorded-at timestamp is required")
        private LocalDateTime recordedAt;

        @NotNull(message = "Record type is required (OBSERVATION or FORECAST)")
        private WeatherRecord.RecordType recordType;

        @DecimalMin(value = "-90.0", message = "Temperature must be at least -90°C")
        @DecimalMax(value = "60.0",  message = "Temperature must be at most 60°C")
        private Double temperatureCelsius;

        @DecimalMin(value = "-90.0", message = "Feels-like temperature must be at least -90°C")
        @DecimalMax(value = "60.0",  message = "Feels-like temperature must be at most 60°C")
        private Double feelsLikeCelsius;

        @Min(value = 0,   message = "Humidity must be between 0 and 100")
        @Max(value = 100, message = "Humidity must be between 0 and 100")
        private Integer humidityPercent;

        @PositiveOrZero(message = "Rainfall must be zero or positive")
        private Double rainfallMm;

        @PositiveOrZero(message = "Wind speed must be zero or positive")
        private Double windSpeedKmh;

        private WeatherRecord.WindDirection windDirection;

        private WeatherRecord.WeatherCondition weatherCondition;

        @PositiveOrZero(message = "Visibility must be zero or positive")
        private Double visibilityKm;

        @Min(value = 0,  message = "UV index must be between 0 and 20")
        @Max(value = 20, message = "UV index must be between 0 and 20")
        private Integer uvIndex;

        private String dataSource;
    }

    // ── API Response ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private String id;
        private String countryCode;
        private String region;
        private Double latitude;
        private Double longitude;

        private LocalDateTime recordedAt;
        private WeatherRecord.RecordType recordType;

        private Double  temperatureCelsius;
        private Double  feelsLikeCelsius;
        private Integer humidityPercent;
        private Double  rainfallMm;
        private Double  windSpeedKmh;
        private WeatherRecord.WindDirection   windDirection;
        private WeatherRecord.WeatherCondition weatherCondition;
        private Double  visibilityKm;
        private Integer uvIndex;
        private String  dataSource;

        private WeatherRecord.WeatherStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}