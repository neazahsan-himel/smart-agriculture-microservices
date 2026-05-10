package com.smartagriculture.aiadvisorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AdvisorDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "Question is required")
        @Size(max = 1000, message = "Question must not exceed 1000 characters")
        private String question;

        @NotNull(message = "Query type is required")
        private QueryType queryType;

        private String farmerId;

        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (e.g. BD, IN)")
        private String countryCode;

        private String region;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String question;
        private QueryType queryType;
        private String advice;
        private String farmerId;
        private String countryCode;
        private String region;
        private LocalDateTime generatedAt;
    }

    public enum QueryType {
        CROP_RECOMMENDATION,
        WEATHER_ALERT,
        PEST_CONTROL,
        PLANTING_SCHEDULE,
        IRRIGATION_ADVICE,
        GENERAL
    }
}